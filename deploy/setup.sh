#!/bin/bash
# 서버 최초 설정. Oracle Linux 9 / Ampere A1 에서 한 번만 돌린다.
# 돌리기 전에 콘솔의 보안 목록에 80, 443 인그레스를 열어 둬야 한다.
set -euo pipefail

DOMAIN="${1:?사용법: bash setup.sh <도메인 또는 IP.nip.io>}"

echo "▶ 1/6  방화벽. 바깥을 보는 것은 Caddy 뿐이라 8080 은 열지 않는다."
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
# 예전 OCI 이미지는 firewalld 와 별개로 iptables 규칙을 들고 있었다. Oracle Linux 9.8
# 에서는 INPUT 체인이 비어 있고 정책이 ACCEPT 라, 여기서 firewalld 만 열면 된다.
sudo firewall-cmd --list-services

echo "▶ 2/6  자바 21"
sudo dnf install -y java-21-openjdk-headless

echo "▶ 3/6  MySQL. 스키마만 만들고 테이블은 Flyway 가 만든다."
sudo dnf install -y mysql-server
sudo systemctl enable --now mysqld
sudo mysql -e "CREATE DATABASE IF NOT EXISTS dam CHARACTER SET utf8mb4;"

echo "▶ 4/6  Caddy. 도메인만 적으면 인증서를 받아 갱신까지 한다."
sudo dnf install -y 'dnf-command(copr)'
sudo dnf copr enable -y @caddy/caddy
sudo dnf install -y caddy
# 로그는 파일이 아니라 journald 로 보낸다. caddy 유닛이 ProtectSystem=full 이라
# /var 가 읽기 전용이고, 파일로 쓰려면 예외를 뚫어야 한다. 로그 시스템이 하나면
# 회전 설정도 journald 것 하나로 끝난다.
#
# 웹은 정적 파일이 아니다. next/image 가 사진을 줄이려면 Node 서버가 떠 있어야 해서
# 3000 번으로 넘긴다.
sudo tee /etc/caddy/Caddyfile >/dev/null <<CADDY
$DOMAIN {
	# 웹과 API 가 같은 오리진이라 CORS 가 통째로 사라진다.
	handle /api/* {
		reverse_proxy localhost:8080
	}
	handle {
		reverse_proxy localhost:3000
	}
}
CADDY
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl enable --now caddy

echo "▶ 5/6  서비스 등록"
sudo useradd -r -s /sbin/nologin dam 2>/dev/null || true
sudo mkdir -p /srv/dam /srv/dam-web && sudo chown -R dam:dam /srv/dam

# 비밀값은 이 파일 하나에 둔다. 서비스 파일에 박으면 systemctl cat 으로 다 보인다.
sudo tee /srv/dam/.env >/dev/null <<ENV
DB_URL=jdbc:mysql://localhost:3306/dam
DB_USERNAME=dam
DB_PASSWORD=${DB_PASSWORD}
ENV
sudo chown dam:dam /srv/dam/.env
sudo chmod 600 /srv/dam/.env
sudo tee /etc/systemd/system/dam.service >/dev/null <<'UNIT'
[Unit]
Description=dam
After=network.target mysqld.service

[Service]
User=dam
WorkingDirectory=/srv/dam
EnvironmentFile=/srv/dam/.env
# -Xms 를 -Xmx 와 같게 두어 JVM 이 힙을 처음부터 쥐고 놓지 않게 한다.
# 12GB 의 20% 인 2.4GB 를 넘겨야 유휴 판정의 메모리 조건이 깨진다. 인스턴스
# 크기를 바꾸면 이 값도 같이 봐야 한다.
ExecStart=/usr/bin/java -Xms3g -Xmx3g -jar /srv/dam/dam.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
sudo systemctl daemon-reload

echo "▶ 6/7  로그가 디스크를 채우지 않게"
# 부트 볼륨이 47GB 뿐이라, 접근 로그가 몇 해 쌓이면 서버가 멈춘다. 지우는 것이
# 아니라 오래된 것부터 버리는 것이라, 최근 것은 늘 남아 있다.
# Caddy 와 스프링이 둘 다 journald 로 보내므로 여기 상한 하나면 끝난다.
# 기본값은 디스크의 10% 까지 쓰는데, 부트 볼륨이 47GB 뿐이라 묶어 둔다.
sudo mkdir -p /etc/systemd/journald.conf.d
sudo tee /etc/systemd/journald.conf.d/dam.conf >/dev/null <<'JOURNAL'
[Journal]
SystemMaxUse=500M
MaxRetentionSec=1month
JOURNAL
sudo systemctl restart systemd-journald

echo "▶ 7/7  회수 방지"
# 오라클은 CPU·네트워크·메모리가 7일 연속 셋 다 20% 미만일 때만 회수한다.
# 셋 다여서 하나만 넘겨도 살아남고, 5단계의 고정 힙이 메모리를 계속 넘긴다.
# 다만 에이전트가 지표를 못 올리면 오라클 눈에는 0 으로 보이므로 켜져 있어야 한다.
sudo systemctl enable --now oracle-cloud-agent 2>/dev/null || true
systemctl is-active oracle-cloud-agent || echo "  ⚠ 콘솔에서 모니터링 플러그인을 켜세요"

echo
echo "남은 것: /srv/dam/.env 를 채우고 jar 와 웹 빌드를 올린 뒤"
echo "  sudo systemctl enable --now dam"
