# views.py
import boto3
from botocore.client import Config  # ✅ 이 라인을 추가해주세요.
from botocore.exceptions import ClientError
from django.http import JsonResponse
import uuid


# views.py
# views.py

import boto3
import os
from botocore.client import Config
from botocore.exceptions import ClientError
from django.http import JsonResponse
from django.conf import settings
import uuid


def generate_s3_presigned_url(request):
    file_name = request.GET.get('fileName')
    if not file_name:
        return JsonResponse({'error': 'fileName is required.'}, status=400)

    object_key = f"uploads/{file_name}-{uuid.uuid4()}"

    # Content-Type을 파일 확장자에 따라 설정
    content_type = 'application/octet-stream'
    if file_name.lower().endswith(('.xlsx', '.xls')):
        content_type = 'application/vnd.ms-excel'
    elif file_name.lower().endswith('.csv'):
        content_type = 'text/csv'

    # AWS 자격 증명을 포함하여 S3 클라이언트 생성
    try:
        s3_client = boto3.client(
            's3',
            region_name=settings.MY_AWS_S3_REGION_NAME,
            aws_access_key_id=settings.MY_AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.MY_AWS_SECRET_ACCESS_KEY,
            config=Config(signature_version='s3v4')
        )

        print(f"S3 Client created successfully")
        print(f"Bucket: {settings.MY_AWS_STORAGE_BUCKET_NAME}")
        print(f"Region: {settings.MY_AWS_S3_REGION_NAME}")
        print(f"Object Key: {object_key}")

    except Exception as e:
        print(f"S3 Client creation failed: {str(e)}")
        return JsonResponse({'error': f'S3 client creation failed: {str(e)}'}, status=500)

    try:
        # Presigned URL 생성 (조건 제거)
        presigned_url = s3_client.generate_presigned_url(
            'put_object',
            Params={
                'Bucket': settings.MY_AWS_STORAGE_BUCKET_NAME,
                'Key': object_key,
                'ContentType': content_type,
            },
            ExpiresIn=600  # 10분
        )

        print(f"presigned_url: {presigned_url}")
        return JsonResponse({
            'presignedUrl': presigned_url,
            'key': object_key,
            'bucket': settings.MY_AWS_STORAGE_BUCKET_NAME,
            'contentType': content_type
        })

    except ClientError as e:
        error_code = e.response['Error']['Code']
        error_message = e.response['Error']['Message']
        print(f"S3 ClientError - Code: {error_code}, Message: {error_message}")

        # 더 구체적인 에러 메시지 제공
        if error_code == 'AccessDenied':
            return JsonResponse({
                'error': 'AWS 권한이 부족합니다. IAM 정책을 확인해주세요.',
                'detail': error_message
            }, status=403)
        elif error_code == 'NoSuchBucket':
            return JsonResponse({
                'error': '버킷을 찾을 수 없습니다.',
                'detail': error_message
            }, status=404)
        else:
            return JsonResponse({
                'error': f'S3 에러: {error_code}',
                'detail': error_message
            }, status=500)

    except Exception as e:
        print(f"Unexpected error: {str(e)}")
        return JsonResponse({'error': f'예상치 못한 에러: {str(e)}'}, status=500)