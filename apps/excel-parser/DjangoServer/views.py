import os
from django.http import HttpResponse

def index(request):

    response_text = f"""
    Hello, world!
    """

    return HttpResponse(response_text, content_type="text/plain")