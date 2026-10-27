import logging
import urllib.request

from django.conf import settings

logger = logging.getLogger(__name__)

INVALIDATE_PATH = "/api/campaign/cache/invalidate"
TIMEOUT_SECONDS = 3


def invalidate_spring_cache(auth_header):
    """
    광고 데이터를 DB에 넣은 뒤 Spring 세그먼트 트리 캐시를 지운다(known-issues C9).
    요청자의 JWT를 그대로 넘겨 Spring이 해당 사용자의 트리만 지우게 한다.
    실패해도 업로드 결과에는 영향을 주지 않고, 성공 여부만 반환한다.
    """
    base_url = getattr(settings, "SPRING_API_URL", "")
    if not base_url or not auth_header:
        logger.warning("SPRING_API_URL 또는 Authorization 헤더가 없어 캐시 무효화를 건너뜀")
        return False

    request = urllib.request.Request(
        base_url.rstrip("/") + INVALIDATE_PATH,
        data=b"",
        headers={"Authorization": auth_header},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return 200 <= response.status < 300
    except Exception as e:
        logger.warning(f"Spring 캐시 무효화 실패: {e}")
        return False
