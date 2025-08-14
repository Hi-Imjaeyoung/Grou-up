from pathlib import Path
import os
import environ

BASE_DIR = Path(__file__).resolve().parent.parent

# .env 파일 로드
env = environ.Env(DEBUG=(bool, False))
environ.Env.read_env(os.path.join(BASE_DIR, ".env"))

# 환경 변수
DEBUG = env("DEBUG")
SECRET_KEY = env("SECRET_KEY")
ALLOWED_HOSTS = ["*"]  # 배포 시 도메인 제한 권장

# 환경 변수에서 값 가져오기 (없으면 기본값 사용)
DEBUG = os.environ.get("DEBUG", env("DEBUG", default=False))
SECRET_KEY = os.environ.get("SECRET_KEY", env("SECRET_KEY", default="your-default-secret-key"))

MY_AWS_ACCESS_KEY_ID =  env('MY_AWS_ACCESS_KEY_ID')
MY_AWS_SECRET_ACCESS_KEY =  env('MY_AWS_SECRET_ACCESS_KEY')
MY_AWS_STORAGE_BUCKET_NAME =  env('MY_AWS_STORAGE_BUCKET_NAME')
MY_AWS_S3_REGION_NAME =  env('MY_AWS_S3_REGION_NAME')


ALLOWED_HOSTS = ["*"]

# Django 앱 설정
INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "DjangoServer.lottoTest",
    "rest_framework",
    "corsheaders",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "DjangoServer.urls"


# 데이터베이스 설정 (MySQL 기준)
DATABASES = {
    "default": {
        "ENGINE": env("DB_ENGINE", default="django.db.backends.mysql"),
        "NAME": env("DB_NAME"),
        "USER": env("DB_USER"),
        "PASSWORD": env("DB_PASSWORD"),
        "HOST": env("DB_HOST"),
        "PORT": env("DB_PORT"),
        "OPTIONS": {
            "charset": "utf8mb4",
            "collation": "utf8mb4_general_ci",
        },
    }
}

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [BASE_DIR / "templates"],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "DjangoServer.wsgi.application"

# 비밀번호 검증
AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

# 국제화 설정
LANGUAGE_CODE = "en-us"
TIME_ZONE = "Asia/Seoul"
USE_I18N = True
USE_TZ = True

# 정적 파일 설정
STATIC_URL = "static/"

# 기본 PK 타입 설정
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# CORS 설정
CORS_ALLOW_ALL_ORIGINS = True
CORS_ALLOWED_ORIGINS = [
    "http://localhost:3000",  # React 앱
    "https://localhost:3000",  # React 앱
    "https://grouup.co.kr",
    "http://grouup.co.kr"
]

