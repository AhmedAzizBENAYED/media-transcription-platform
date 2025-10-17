"""
Whisper AI Transcription Service
A standalone Python service that provides transcription endpoints using OpenAI Whisper

Installation:
pip install flask whisper torch
"""

from flask import Flask, request, jsonify
import whisper
import tempfile
import os
import time
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Load Whisper model (base model for balance between speed and accuracy)
# Options: tiny, base, small, medium, large
MODEL_SIZE = os.getenv('WHISPER_MODEL', 'base')
logger.info(f"Loading Whisper model: {MODEL_SIZE}")
model = whisper.load_model(MODEL_SIZE)
logger.info("Whisper model loaded successfully")

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'model': MODEL_SIZE,
        'service': 'Whisper Transcription Service'
    }), 200

@app.route('/transcribe', methods=['POST'])
def transcribe():
    """
    Transcribe audio/video file

    Expected: multipart/form-data with 'file' field
    Returns: JSON with transcription text, language, and metadata
    """
    logger.info("Received transcription request")

    if 'file' not in request.files:
        logger.error("No file provided in request")
        return jsonify({'error': 'No file provided'}), 400

    file = request.files['file']

    if file.filename == '':
        logger.error("Empty filename")
        return jsonify({'error': 'Empty filename'}), 400

    try:
        # Save uploaded file temporarily
        with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as temp_file:
            temp_path = temp_file.name
            file.save(temp_path)
            logger.info(f"Saved temporary file: {temp_path}")

        # Start transcription
        start_time = time.time()
        logger.info(f"Starting transcription for: {file.filename}")

        # Transcribe with Whisper
        result = model.transcribe(
            temp_path,
            language=None,  # Auto-detect language
            task='transcribe',  # or 'translate' for translation to English
            verbose=False
        )

        processing_time = int((time.time() - start_time) * 1000)  # Convert to ms

        # Clean up temporary file
        os.unlink(temp_path)
        logger.info(f"Transcription completed in {processing_time}ms")

        # Prepare response
        response = {
            'text': result['text'].strip(),
            'language': result.get('language', 'unknown'),
            'confidence': 0.95,  # Whisper doesn't provide word-level confidence, using estimate
            'processingTimeMs': processing_time,
            'segments': len(result.get('segments', [])),
            'model': MODEL_SIZE
        }

        logger.info(f"Transcription result: {len(response['text'])} characters, language: {response['language']}")

        return jsonify(response), 200

    except Exception as e:
        logger.error(f"Error during transcription: {str(e)}", exc_info=True)

        # Clean up temp file on error
        if 'temp_path' in locals() and os.path.exists(temp_path):
            os.unlink(temp_path)

        return jsonify({
            'error': 'Transcription failed',
            'message': str(e)
        }), 500

@app.route('/transcribe/batch', methods=['POST'])
def transcribe_batch():
    """
    Transcribe multiple files in batch

    Expected: multipart/form-data with multiple 'files' fields
    """
    logger.info("Received batch transcription request")

    if 'files' not in request.files:
        return jsonify({'error': 'No files provided'}), 400

    files = request.files.getlist('files')
    results = []

    for file in files:
        try:
            with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as temp_file:
                temp_path = temp_file.name
                file.save(temp_path)

            start_time = time.time()
            result = model.transcribe(temp_path, verbose=False)
            processing_time = int((time.time() - start_time) * 1000)

            os.unlink(temp_path)

            results.append({
                'filename': file.filename,
                'text': result['text'].strip(),
                'language': result.get('language', 'unknown'),
                'processingTimeMs': processing_time,
                'status': 'success'
            })

        except Exception as e:
            logger.error(f"Error transcribing {file.filename}: {str(e)}")
            results.append({
                'filename': file.filename,
                'error': str(e),
                'status': 'failed'
            })

    return jsonify({
        'totalFiles': len(files),
        'results': results
    }), 200

@app.route('/models', methods=['GET'])
def list_models():
    """List available Whisper models"""
    return jsonify({
        'current': MODEL_SIZE,
        'available': ['tiny', 'base', 'small', 'medium', 'large'],
        'info': {
            'tiny': 'Fastest, lowest accuracy (~1GB RAM)',
            'base': 'Good balance (~1GB RAM)',
            'small': 'Better accuracy (~2GB RAM)',
            'medium': 'High accuracy (~5GB RAM)',
            'large': 'Best accuracy (~10GB RAM)'
        }
    }), 200

if __name__ == '__main__':
    # Get configuration from environment
    host = os.getenv('HOST', '0.0.0.0')
    port = int(os.getenv('PORT', 8001))
    debug = os.getenv('DEBUG', 'False').lower() == 'true'

    logger.info(f"Starting Whisper Transcription Service on {host}:{port}")
    logger.info(f"Model: {MODEL_SIZE}")

    app.run(host=host, port=port, debug=debug, threaded=True)