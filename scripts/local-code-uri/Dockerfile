FROM python:3.9

WORKDIR /usr/local/bin

RUN pip install --upgrade pip && pip install --no-cache-dir cfn_flip

COPY local_code_uri.py .

CMD ["local_code_uri.py"]

ENTRYPOINT ["python3"]