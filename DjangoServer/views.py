import os
from django.http import HttpResponse

def index(request):
    db_engine = os.environ["DB_ENGINE"]
    db_host = os.environ["DB_HOST"]
    db_name = os.environ["DB_NAME"]
    db_user = os.environ["DB_USER"]
    db_password = os.environ["DB_PASSWORD"]
    db_port = os.environ["DB_PORT"]

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