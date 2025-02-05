from pathlib import Path
import os
import environ

# BASE_DIR 설정
BASE_DIR = Path(__file__).resolve().parent.parent

# 환경 변수 설정
env = environ.Env(DEBUG=(bool, False))

# .env 파일이 있으면 로드
env_path = os.path.join(BASE_DIR, ".env.prod")
if os.path.exists(env_path):
    environ.Env.read_env(env_path)

# 환경 변수에서 값 가져오기 (없으면 기본값 사용)
DEBUG = os.environ.get("DEBUG", env("DEBUG", default=False))
SECRET_KEY = os.environ.get("SECRET_KEY", env("SECRET_KEY", default="your-default-secret-key"))

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

# 데이터베이스 설정
DATABASES = {
    "default": {
        "ENGINE": os.environ.get("DB_ENGINE", env("DB_ENGINE", default="mysql.connector.django")),
        "NAME": os.environ.get("DB_NAME", env("DB_NAME", default="secretPang")),
        "USER": os.environ.get("DB_USER", env("DB_USER", default="root")),
        "PASSWORD": os.environ.get("DB_PASSWORD", env("DB_PASSWORD", default="1234")),
        "HOST": os.environ.get("DB_HOST", env("DB_HOST", default="localhost")),
        "PORT": os.environ.get("DB_PORT", env("DB_PORT", default="3306")),
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
    "https://grouup.co.kr",
    "http://grouup.co.kr"
]
