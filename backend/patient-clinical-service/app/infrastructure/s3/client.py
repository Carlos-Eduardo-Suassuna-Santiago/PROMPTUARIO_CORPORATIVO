from __future__ import annotations

import logging
import boto3
from botocore.exceptions import ClientError
from app.config import settings

logger = logging.getLogger(__name__)


def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=settings.S3_ENDPOINT,
        aws_access_key_id=settings.S3_ACCESS_KEY,
        aws_secret_access_key=settings.S3_SECRET_KEY,
        region_name=settings.S3_REGION,
    )


def ensure_s3_bucket(bucket_name: str) -> None:
    """Ensure that the given S3 bucket exists."""
    try:
        s3 = get_s3_client()
        try:
            s3.head_bucket(Bucket=bucket_name)
        except ClientError:
            s3.create_bucket(Bucket=bucket_name)
            logger.info("S3 bucket created: %s", bucket_name)
    except Exception as e:
        logger.warning("Could not ensure S3 bucket %s: %s", bucket_name, e)
