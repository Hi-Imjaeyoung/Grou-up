
import os
from django.http import HttpResponse

def index(request):
    db_host = os.environ.get("DB_HOST", "No DB_HOST found")
    db_user = os.environ.get("DB_USER", "No DB_USER found")

    response_text = f"Hello, world!\nDB_USER: {db_user}\nDB_HOST: {db_host}"
    return HttpResponse(response_text, content_type="text/plain")
