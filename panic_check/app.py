import hashlib
import sqlite3
from flask import Flask, request, jsonify, render_template  # type: ignore
from flask_cors import CORS  # type: ignore

app = Flask(__name__)
CORS(app)

# ---------- Database initialization ----------
def init_db():
    conn = sqlite3.connect('traceit.db')
    c = conn.cursor()
    c.execute('''
        CREATE TABLE IF NOT EXISTS forwards (
            hash TEXT PRIMARY KEY,
            count INTEGER DEFAULT 1,
            first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    conn.commit()
    conn.close()

init_db()

# ---------- NLP: sensationalism score (keyword-based) ----------
SENSATIONAL_WORDS = [
    'urgent', 'warning', 'shocking', 'secret', 'they don\'t want you to know',
    'viral', 'immediately', 'share with', 'forward', 'delete', 'hidden',
    'scam', 'alert', 'danger', 'deadly', 'miracle', 'cure', 'big pharma',
    'government hiding', 'exposed', 'breaking', 'must read', 'goes viral',
    'send to everyone', 'this is not a joke', '100% true', 'doctors hate',
    'what happens next', 'you won\'t believe', 'mind blowing', 'conspiracy'
]

def sensationalism_score(text):
    text_lower = text.lower()
    matches = [word for word in SENSATIONAL_WORDS if word in text_lower]
    match_count = len(matches)
    if match_count >= 5:
        return 'High'
    elif match_count >= 2:
        return 'Medium'
    else:
        return 'Low'

def extract_search_query(text):
    words = text.split()
    query = ' '.join(words[:5])
    query = ''.join(c for c in query if c.isalnum() or c.isspace())
    return query if query else "fact check"

# ---------- API endpoint ----------
@app.route('/api/check', methods=['POST'])
def check_message():
    data = request.get_json()
    text = data.get('text', '').strip()

    if not text:
        return jsonify({'error': 'No text provided'}), 400

    normalized = ' '.join(text.split()).lower()
    text_hash = hashlib.sha256(normalized.encode('utf-8')).hexdigest()

    conn = sqlite3.connect('traceit.db')
    c = conn.cursor()
    c.execute('''
        INSERT INTO forwards (hash, count, last_seen) VALUES (?, 1, CURRENT_TIMESTAMP)
        ON CONFLICT(hash) DO UPDATE SET
            count = count + 1,
            last_seen = CURRENT_TIMESTAMP
    ''', (text_hash,))
    conn.commit()

    c.execute('SELECT count FROM forwards WHERE hash = ?', (text_hash,))
    count = c.fetchone()[0]
    conn.close()

    score = sensationalism_score(text)
    short_hash = text_hash[:8]  # type: ignore
    search_query = extract_search_query(text)

    return jsonify({
        'hash': short_hash,
        'forward_count': count,
        'sensationalism_score': score,
        'search_query': search_query
    })

# Optional: serve a simple frontend (not needed for Android, but useful for testing)
@app.route('/')
def index():
    return render_template('index.html')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
