#!/usr/bin/env bash
# 서버 안에서 도는 부분. jar 를 바꿔 끼우고, 뜨지 않으면 되돌린다.
# CD 가 부르지만 손으로도 돌릴 수 있다:  bash release.sh <커밋sha>
set -euo pipefail

NEW=/tmp/dam-new.jar
LIVE=/srv/dam/dam.jar
PREV=/srv/dam/dam.jar.prev
HEALTH=http://localhost:8080/api/v1/actuator/health
SHA="${1:-manual}"

[ -f "$NEW" ] || { echo "올라온 jar 가 없다: $NEW"; exit 1; }

sudo cp "$LIVE" "$PREV" 2>/dev/null || true
sudo mv "$NEW" "$LIVE"
sudo chown dam:dam "$LIVE"

# 어느 배포에서 난 오류인지 Sentry 가 알 수 있게 커밋을 심는다.
sudo sed -i "/^SENTRY_RELEASE=/d" /etc/dam.env
echo "SENTRY_RELEASE=$SHA" | sudo tee -a /etc/dam.env > /dev/null

sudo systemctl restart dam

for _ in $(seq 1 30); do
  sleep 2
  if curl -sf "$HEALTH" > /dev/null; then
    echo "떴다: $SHA"
    sudo rm -f "$PREV"
    exit 0
  fi
done

# 되돌리지 않으면 배포 한 번 잘못해서 서버가 내려간 채로 남는다.
echo "60초 안에 뜨지 않았다. 되돌린다."
sudo journalctl -u dam -n 40 --no-pager
if [ -f "$PREV" ]; then
  sudo mv "$PREV" "$LIVE"
  sudo chown dam:dam "$LIVE"
  sudo systemctl restart dam
  echo "이전 jar 로 돌아갔다"
else
  echo "돌아갈 jar 가 없다. 첫 배포였던 듯하다"
fi
exit 1
