import os
from django.http import HttpResponse

def index(request):
    db_engine = os.environ.get("DB_ENGINE", "No DB_ENGINE found")
    db_host = os.environ.get("DB_HOST", "No DB_HOST found")
    db_name = os.environ.get("DB_NAME", "No DB_NAME found")
    db_user = os.environ.get("DB_USER", "No DB_USER found")
    db_password = os.environ.get("DB_PASSWORD", "No DB_PASSWORD found")
    db_port = os.environ.get("DB_PORT", "No DB_PORT found")

    response_text = f"""
    Hello, world!
    DB_ENGINE: {db_engine}
    DB_HOST: {db_host}
    DB_NAME: {db_name}
    DB_USER: {db_user}
    DB_PASSWORD: {db_password}
    DB_PORT: {db_port}
    """

    return HttpResponse(response_text, content_type="text/plain")
