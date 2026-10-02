FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
RUN useradd --system --uid 10001 --create-home securebiz
WORKDIR /app
COPY pyproject.toml ./
COPY app ./app
COPY features ./features
COPY preprocessing ./preprocessing
COPY training ./training
COPY evaluation ./evaluation
COPY models ./models
COPY datasets ./datasets
RUN pip install --no-cache-dir .
USER securebiz
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
