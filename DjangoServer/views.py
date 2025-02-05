from django.http import HttpResponse
import os
def index(request):
    b =  os.environ.get("DB_HOST")
    a=  os.environ.get("DB_USER")
    return HttpResponse("Hello, world!", a, b)