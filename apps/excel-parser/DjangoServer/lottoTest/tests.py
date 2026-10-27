from unittest import mock

from django.test import SimpleTestCase, override_settings

from .cacheInvalidation import invalidate_spring_cache


class InvalidateSpringCacheTest(SimpleTestCase):

    @override_settings(SPRING_API_URL="http://spring:8080/")
    @mock.patch("urllib.request.urlopen")
    def test_forwards_jwt_to_invalidate_endpoint(self, urlopen):
        urlopen.return_value.__enter__.return_value.status = 200

        result = invalidate_spring_cache("Bearer token")

        self.assertTrue(result)
        request = urlopen.call_args.args[0]
        self.assertEqual(request.full_url, "http://spring:8080/api/campaign/cache/invalidate")
        self.assertEqual(request.get_method(), "POST")
        self.assertEqual(request.get_header("Authorization"), "Bearer token")

    @override_settings(SPRING_API_URL="http://spring:8080")
    @mock.patch("urllib.request.urlopen", side_effect=OSError("connection refused"))
    def test_failure_does_not_raise(self, urlopen):
        self.assertFalse(invalidate_spring_cache("Bearer token"))

    @override_settings(SPRING_API_URL="")
    @mock.patch("urllib.request.urlopen")
    def test_skips_without_url(self, urlopen):
        self.assertFalse(invalidate_spring_cache("Bearer token"))
        urlopen.assert_not_called()
