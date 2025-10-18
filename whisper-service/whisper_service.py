"""
Whisper AI Transcription Service
Provides real speech-to-text transcription using OpenAI Whisper
"""

from flask import Flask, request, jsonify
import whisper
import tempfile
import os
import time
import logging
import traceback

app = Flask(__name__)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Load Whisper model
MODEL_SIZE = os.getenv('WHISPER_MODEL', 'base')
logger.info(f"Loading Whisper model: {MODEL_SIZE}")
logger.info("This may take a few minutes on first run...")

try:
    model = whisper.load_model(MODEL_SIZE)
    logger.info(f"✓ Whisper model '{MODEL_SIZE}' loaded successfully")
except Exception as e:
    logger.error(f"Failed to load Whisper model: {e}")
    raise

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'model': MODEL_SIZE,
        'service': 'Whisper Transcription Service',
        'version': '1.0.0'
    }), 200

@app.route('/transcribe', methods=['POST'])
def transcribe():
    """
    Transcribe audio/video file

    Request: multipart/form-data with 'file' field
    Response: JSON with transcription text and metadata
    """
    logger.info("=" * 50)
    logger.info("Received transcription request")

    # Validate request
    if 'file' not in request.files:
        logger.error("No file provided in request")
        return jsonify({'error': 'No file provided'}), 400

    file = request.files['file']

    if file.filename == '':
        logger.error("Empty filename")
        return jsonify({'error': 'Empty filename'}), 400

    temp_path = None

    try:
        # Save uploaded file temporarily
        file_ext = os.path.splitext(file.filename)[1]
        with tempfile.NamedTemporaryFile(delete=False, suffix=file_ext) as temp_file:
            temp_path = temp_file.name
            file.save(temp_path)
            file_size = os.path.getsize(temp_path)

        logger.info(f"File saved: {file.filename} ({file_size} bytes)")
        logger.info(f"Temporary path: {temp_path}")

        # Start transcription
        start_time = time.time()
        logger.info(f"Starting Whisper transcription...")

        # Transcribe with Whisper
        result = model.transcribe(
            temp_path,
            language=None,  # Auto-detect language
            task='transcribe',
            fp16=False,  # Use FP32 for CPU compatibility
            verbose=False
        )

        processing_time = int((time.time() - start_time) * 1000)

        # Extract results
        transcript_text = result['text'].strip()
        detected_language = result.get('language', 'unknown')
        segments = result.get('segments', [])

        # Calculate average confidence if available
        confidence = 0.0
        if segments:
            confidences = [seg.get('no_speech_prob', 0) for seg in segments]
            confidence = 1.0 - (sum(confidences) / len(confidences))
        else:
            confidence = 0.85  # Default confidence

        logger.info(f"✓ Transcription completed in {processing_time}ms")
        logger.info(f"  Language: {detected_language}")
        logger.info(f"  Length: {len(transcript_text)} characters")
        logger.info(f"  Segments: {len(segments)}")
        logger.info(f"  Confidence: {confidence:.2f}")

        # Prepare response
        response = {
            'text': transcript_text,
            'language': detected_language,
            'confidence': round(confidence, 3),
            'processingTimeMs': processing_time,
            'segments': len(segments),
            'model': MODEL_SIZE,
            'filename': file.filename,
            'fileSize': file_size
        }

        logger.info("=" * 50)
        return jsonify(response), 200

    except Exception as e:
        logger.error(f"Error during transcription: {str(e)}")
        logger.error(traceback.format_exc())

        return jsonify({
            'error': 'Transcription failed',
            'message': str(e),
            'type': type(e).__name__
        }), 500

    finally:
        # Clean up temp file
        if temp_path and os.path.exists(temp_path):
            try:
                os.unlink(temp_path)
                logger.info(f"Cleaned up temporary file: {temp_path}")
            except Exception as e:
                logger.warning(f"Failed to delete temp file: {e}")

@app.route('/models', methods=['GET'])
def list_models():
    """List available Whisper models and current model info"""
    return jsonify({
        'current': MODEL_SIZE,
        'available': ['tiny', 'base', 'small', 'medium', 'large'],
        'info': {
            'tiny': {
                'description': 'Fastest, lowest accuracy',
                'params': '39M',
                'vram': '~1GB',
                'speed': 'Very Fast'
            },
            'base': {
                'description': 'Good balance (recommended)',
                'params': '74M',
                'vram': '~1GB',
                'speed': 'Fast'
            },
            'small': {
                'description': 'Better accuracy',
                'params': '244M',
                'vram': '~2GB',
                'speed': 'Medium'
            },
            'medium': {
                'description': 'High accuracy',
                'params': '769M',
                'vram': '~5GB',
                'speed': 'Slow'
            },
            'large': {
                'description': 'Best accuracy',
                'params': '1550M',
                'vram': '~10GB',
                'speed': 'Very Slow'
            }
        }
    }), 200

@app.route('/info', methods=['GET'])
def info():
    """Get service information"""
    import torch
    return jsonify({
        'service': 'Whisper Transcription Service',
        'version': '1.0.0',
        'model': MODEL_SIZE,
        'torch_version': torch.__version__,
        'cuda_available': torch.cuda.is_available(),
        'device': 'cuda' if torch.cuda.is_available() else 'cpu'
    }), 200

if __name__ == '__main__':
    host = os.getenv('HOST', '0.0.0.0')
    port = int(os.getenv('PORT', 8001))
    debug = os.getenv('DEBUG', 'False').lower() == 'true'

    logger.info("=" * 50)
    logger.info(f"Starting Whisper Transcription Service")
    logger.info(f"  Host: {host}")
    logger.info(f"  Port: {port}")
    logger.info(f"  Model: {MODEL_SIZE}")
    logger.info(f"  Debug: {debug}")
    logger.info("=" * 50)

    app.run(host=host, port=port, debug=debug, threaded=True)