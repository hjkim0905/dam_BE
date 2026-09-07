#!/bin/bash
# A1 자리가 날 때까지 계속 두드린다. 항상 켜져 있는 곳에서 돌린다.
#
# 미리 할 것: oci setup config 로 ~/.oci/config 를 만들어 두기.
# OCID 는 직접 넣지 않는다. 손으로 옮기면 틀리고, CLI 가 이미 알고 있다.
set -uo pipefail

# 오라클은 용량 폴링을 막으려고 launch API 에 따로 제한을 걸어 뒀다. 짧게 잡으면
# 429 에 자주 걸리고, 걸린 동안은 자리가 나도 못 본다. 그래서 간격을 줄이는 것이
# 실제 시도 횟수를 늘리는지는 로그로 재 봐야 안다.
INTERVAL="${INTERVAL:-10}"
BACKOFF_START="${BACKOFF_START:-30}"
BACKOFF_MAX="${BACKOFF_MAX:-30}"

OCPUS="${OCPUS:-1}"
MEMORY="${MEMORY:-6}"
NAME="${NAME:-dam}"
WEBHOOK="${WEBHOOK:-}"
# 인스턴스에 심을 공개키. 잡아 주는 기계와 접속할 기계가 다를 수 있으므로
# 이 자리의 키를 쓰지 않고 밖에서 지정한다. 그래야 잡은 뒤 이 기계를 버릴 수 있다.
PUBKEY_FILE="${PUBKEY_FILE:-$HOME/.ssh/id_ed25519.pub}"

say() { echo "[$(date '+%m-%d %H:%M:%S')] $*"; }

# 시작 단계에서 죽어도 알려야 한다. 조용히 사라지면 돌고 있는 줄 알고 며칠을 버린다.
die() { say "$1"; notify "담. 시작하지 못했습니다: $1"; exit 1; }

# 두 벌이 같이 돌면 두드리는 횟수가 배로 늘어 429 만 더 맞는다.
# flock 이 없다고 그만두지는 않는다. 조용히 아무것도 안 하는 것이 제일 나쁜 실패라,
# 잠금 없이라도 도는 편이 낫다.
if command -v flock >/dev/null 2>&1; then
  exec 9>"$HOME/.grab-a1.lock"
  flock -n 9 || { say "이미 돌고 있어서 그만둡니다"; exit 0; }
else
  say "flock 이 없어 중복 실행을 막지 못합니다. 두 벌이 돌고 있지 않은지 확인하세요"
fi

notify() {
  [ -n "$WEBHOOK" ] || return 0
  local body
  body=$(printf '%s' "$1" | python3 -c 'import json,sys; print(json.dumps({"content": sys.stdin.read()}))')
  curl -sS -X POST -H 'Content-Type: application/json' -d "$body" "$WEBHOOK" >/dev/null || true
}

say "필요한 것을 찾는 중"
# 하위 컴파트먼트를 안 만들었으면 목록이 비어서 거기서는 못 얻는다. config 에 있다.
TENANCY=$(awk -F= '/^tenancy=/{print $2}' "${OCI_CONFIG_FILE:-$HOME/.oci/config}" | tr -d ' \r')
[ -n "$TENANCY" ] || die "~/.oci/config 에서 tenancy 를 못 읽었습니다"
oci iam region-subscription list >/dev/null 2>&1 \
  || die "oci 인증 실패. ~/.oci/config 를 확인하세요"

# 공용 IP 를 붙일 수 있는 서브넷이어야 한다. 사설 서브넷에 띄우면 바깥에서 못 닿는다.
SUBNET=$(oci network subnet list --compartment-id "$TENANCY" \
  --query 'data[?"prohibit-public-ip-on-vnic"==`false`]|[0].id' --raw-output)
[ -n "$SUBNET" ] && [ "$SUBNET" != "null" ] \
  || die "공용 서브넷이 없습니다. VCN 마법사로 먼저 만드세요"

AD=$(oci network subnet get --subnet-id "$SUBNET" --query 'data."availability-domain"' --raw-output)
# 리전 전체를 덮는 서브넷은 AD 가 비어 있다. 그때는 첫 AD 를 쓴다.
[ -n "$AD" ] && [ "$AD" != "null" ] || AD=$(oci iam availability-domain list --query 'data[0].name' --raw-output)

IMAGE=$(oci compute image list --compartment-id "$TENANCY" \
  --operating-system "Oracle Linux" --operating-system-version "9" \
  --shape VM.Standard.A1.Flex --sort-by TIMECREATED \
  --query 'data[0].id' --raw-output)
[ -n "$IMAGE" ] && [ "$IMAGE" != "null" ] || die "ARM 용 Oracle Linux 9 이미지를 못 찾았습니다"

KEY=$(cat "$PUBKEY_FILE" 2>/dev/null || echo "")
[ -n "$KEY" ] || die "공개키를 못 읽었습니다: $PUBKEY_FILE"

say "AD=$AD  서브넷=...${SUBNET: -12}  이미지=...${IMAGE: -12}"
say "심을 키: $(echo "$KEY" | awk '{print $NF}')"
say "${INTERVAL}초마다 두드립니다. 로그는 이 파일에 쌓입니다."
notify "담. A1 자리를 ${INTERVAL}초마다 두드리기 시작합니다."

# 이미 잡아 둔 것이 있으면 두 번 만들지 않는다. 무료 한도를 넘기면 과금이고,
# 앞선 시도가 성공했는데 대기 중에 끊겨 못 알아챈 경우도 여기서 걸린다.
already() {
  # RUNNING 만 보면 안 된다. 만든 직후(PROVISIONING)나 꺼 둔 것(STOPPED)을 놓치면
  # 두 번째를 만들게 되는데, 2 OCPU 12GB 가 무료 한도 전부라 그것은 과금이다.
  oci compute instance list --compartment-id "$TENANCY" --display-name "$NAME" \
    --query 'data[?"lifecycle-state"!=`TERMINATED` && "lifecycle-state"!=`TERMINATING`]|[0].id' \
    --raw-output 2>/dev/null
}

FOUND=$(already)
if [ -n "$FOUND" ] && [ "$FOUND" != "null" ]; then
  say "이미 돌고 있는 것이 있습니다: ...${FOUND: -12}"
  notify "담. 이미 인스턴스가 있어서 두드리지 않습니다."
  exit 0
fi

TRIES=0
BACKOFF=$BACKOFF_START
LAST_PING=0

while true; do
  TRIES=$((TRIES + 1))
  OUT=$(oci compute instance launch \
    --availability-domain "$AD" \
    --compartment-id "$TENANCY" \
    --shape VM.Standard.A1.Flex \
    --shape-config "{\"ocpus\":$OCPUS,\"memoryInGBs\":$MEMORY}" \
    --image-id "$IMAGE" \
    --subnet-id "$SUBNET" \
    --assign-public-ip true \
    --display-name "$NAME" \
    --metadata "{\"ssh_authorized_keys\":\"$KEY\"}" \
    --wait-for-state RUNNING 2>&1)
  CODE=$?

  if [ $CODE -eq 0 ]; then
    say "잡았습니다. ${TRIES}번째 시도"
    notify "담. A1 인스턴스를 잡았습니다 (${TRIES}번째 시도)"
    echo "$OUT"
    exit 0
  fi

  case "$OUT" in
    *"Out of host capacity"*|*"Out of capacity"*)
      say "${TRIES}번째, 아직 자리 없음"
      # 10초마다 알리면 하루 8천 개다. 디스코드가 웹후크를 막고 폰도 못 쓴다.
      # 살아 있다는 것만 알면 되므로 5분에 한 번으로 묶는다.
      NOW=$(date +%s)
      if [ $(( NOW - LAST_PING )) -ge 300 ]; then
        notify "${TRIES}번째까지 두드렸습니다. 아직 자리 없음"
        LAST_PING=$NOW
      fi
      BACKOFF=$BACKOFF_START
      sleep "$INTERVAL" ;;
    *TooManyRequests*|*"status\": 429"*)
      # 같은 간격으로 돌아가면 또 걸린다. 걸릴수록 더 물러선다.
      say "요청이 많다고 합니다. ${BACKOFF}초 쉽니다"
      notify "요청이 많다고 해서 ${BACKOFF}초 쉽니다"
      sleep "$BACKOFF"
      BACKOFF=$(( BACKOFF * 2 ))
      [ $BACKOFF -gt $BACKOFF_MAX ] && BACKOFF=$BACKOFF_MAX ;;
    *LimitExceeded*)
      # 무료 한도를 넘겼다. 기다린다고 풀리지 않으니 왜 멈췄는지 남기고 끝낸다.
      say "무료 한도를 넘었습니다. 이미 쓰고 있는 인스턴스를 확인하세요"
      echo "$OUT"
      notify "담. 무료 한도를 넘어 멈췄습니다. 쓰고 있는 인스턴스를 확인하세요"
      exit 1 ;;
    *NotAuthorized*|*InvalidParameter*)
      # 설정이 틀린 것이다. 계속 두드려 봐야 같은 답만 온다.
      say "요청이 거부됐습니다. 설정을 확인하세요"
      echo "$OUT"
      notify "담. 요청이 거부돼 멈췄습니다. 로그를 확인하세요"
      exit 1 ;;
    *)
      # 만들어 놓고 대기 중에 끊긴 경우가 있다. 멈추기 전에 실물을 확인한다.
      FOUND=$(already)
      if [ -n "$FOUND" ] && [ "$FOUND" != "null" ]; then
        say "응답은 실패였지만 인스턴스는 만들어져 있습니다"
        notify "담. A1 인스턴스를 잡았습니다 (응답은 실패로 왔지만 실물이 있습니다)"
        exit 0
      fi
      say "예상 못한 응답이라 멈춥니다"
      echo "$OUT"
      notify "담. 재시도를 멈췄습니다. 로그를 확인하세요"
      exit 1 ;;
  esac
done
