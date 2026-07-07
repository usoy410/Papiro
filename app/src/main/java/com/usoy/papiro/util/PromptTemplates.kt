package com.usoy.papiro.util

object PromptTemplates {

    fun getQuizPrompt(quizType: String, difficultyLevel: String, numberOfItems: String, contextText: String): String {
        val mcTemplate = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Notebook Quiz</title>
    <style>
        :root {
            --bg-color: #f4f1ea;
            --paper-color: #fcfbfa;
            --rule-color: #e0dad0;
            --margin-line: #ff7073;
            --text-color: #2b2b2b;
            --text-light: #666;
            --primary: #2e5cb8;
            --success: #2e7d32;
            --error: #c62828;
            --border-radius: 12px;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
        body {
            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
            background-color: var(--bg-color); color: var(--text-color);
            line-height: 1.6; padding: 12px; display: flex; justify-content: center; align-items: flex-start;
            min-height: 100vh; overflow-x: hidden;
        }
        .notebook {
            width: 100%; max-width: 600px; background-color: var(--paper-color);
            border-radius: var(--border-radius); box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
            border: 1px solid #dfdcd3; position: relative; min-height: 85vh; display: flex; flex-direction: column; overflow: hidden;
        }
        .notebook::before {
            content: ''; position: absolute; top: 0; left: 0; width: 8px; height: 100%;
            background: linear-gradient(to right, #333 30%, #555 70%, #222 100%); z-index: 10;
        }
        .paper-content {
            padding: 24px 16px 24px 36px;
            background-image: linear-gradient(var(--rule-color) 1px, transparent 1px);
            background-size: 100% 28px; position: relative; flex-grow: 1; display: flex; flex-direction: column;
        }
        .paper-content::before {
            content: ''; position: absolute; top: 0; left: 26px; width: 2px; height: 100%; background-color: var(--margin-line); opacity: 0.8;
        }
        h1, h2, h3 { color: var(--text-color); margin-bottom: 16px; font-weight: 600; }
        .screen { display: none; flex-direction: column; height: 100%; animation: fadeIn 0.3s ease-in-out; }
        .screen.active { display: flex; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
        .progress { font-size: 0.9rem; font-weight: 600; color: var(--primary); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; }
        .question-text { font-size: 1.25rem; font-weight: 500; margin-bottom: 24px; line-height: 1.4; color: #111; }
        .options-list { list-style: none; display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
        .option-btn {
            background-color: rgba(255, 255, 255, 0.9); border: 2px solid #e0dad0; border-radius: 8px;
            padding: 16px; text-align: left; font-size: 1.05rem; cursor: pointer; transition: all 0.2s ease;
            color: var(--text-color); display: flex; align-items: center; min-height: 56px;
        }
        .option-btn:active { transform: scale(0.98); background-color: #eae5da; }
        .option-btn.selected { background-color: #e2ecf7; border-color: var(--primary); font-weight: 600; }
        .btn-container { margin-top: auto; padding-top: 20px; padding-bottom: 10px; }
        .primary-btn {
            width: 100%; background-color: var(--primary); color: white; border: none; padding: 16px;
            font-size: 1.1rem; font-weight: 600; border-radius: 8px; cursor: pointer; text-align: center;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1); transition: transform 0.1s; min-height: 56px;
        }
        .primary-btn:active { transform: translateY(2px); box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .primary-btn:disabled { background-color: #cccccc; cursor: not-allowed; box-shadow: none; transform: none; }
        .score-display { text-align: center; margin: 30px 0; background: #fff; padding: 20px; border-radius: 12px; border: 1px solid #eee; }
        .score-number { font-size: 4rem; font-weight: bold; color: var(--primary); }
        .review-container { display: flex; flex-direction: column; gap: 20px; margin-top: 16px; }
        .review-item { background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #e0dad0; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
        .review-question { font-weight: 600; margin-bottom: 12px; font-size: 1.1rem; }
        .review-answer { padding: 12px; border-radius: 6px; margin-bottom: 8px; font-size: 0.95rem; font-weight: 500; }
        .review-answer.user-wrong { background-color: #ffebee; border-left: 4px solid var(--error); color: #c62828; }
        .review-answer.correct-assigned { background-color: #e8f5e9; border-left: 4px solid var(--success); color: #2e7d32; }
        .explanation-box { background-color: #f8f9fa; border: 1px solid #e9ecef; padding: 14px; border-radius: 6px; font-size: 0.95rem; margin-top: 12px; color: #495057; }
        .explanation-title { font-weight: 700; margin-bottom: 6px; display: block; color: #212529; text-transform: uppercase; font-size: 0.8rem; letter-spacing: 0.5px; }
    </style>
</head>
<body>
    <div class="notebook">
        <div class="paper-content">
            <div id="quiz-screen" class="screen active">
                <div class="progress">Question <span id="current-idx">1</span> / <span id="total-idx">3</span></div>
                <div class="question-text" id="question-txt">Loading...</div>
                <div class="options-list" id="options-box"></div>
                <div class="btn-container">
                    <button class="primary-btn" id="next-btn" onclick="quizApp.nextQuestion()" disabled>Next</button>
                </div>
            </div>
            <div id="result-screen" class="screen">
                <h2>Quiz Summary</h2>
                <div class="score-display">
                    <p style="color: var(--text-light); font-weight: 600; text-transform: uppercase; letter-spacing: 1px; font-size: 0.9rem;">Final Score</p>
                    <div class="score-number"><span id="score-txt">0</span>/<span id="total-score-txt">0</span></div>
                </div>
                <h3>Review</h3>
                <div class="review-container" id="review-box"></div>
                <div class="btn-container" style="margin-top: 20px;">
                    <button class="primary-btn" onclick="quizApp.resetQuiz()">Retake Quiz</button>
                </div>
            </div>
        </div>
    </div>
    <script>
        // //QUIZ_DATA_PLACEHOLDER//
        const quizData = [
            { question: "Sample", options: ["A", "B"], correct: 0, explanation: "Exp" }
        ];

        class QuizEngine {
            constructor(data) { this.data = data; this.currentQuestionIndex = 0; this.userAnswers = []; }
            init() { this.loadQuestion(); }
            switchScreen(screenId) {
                document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
                document.getElementById(screenId).classList.add('active');
                window.scrollTo(0,0);
            }
            loadQuestion() {
                const q = this.data[this.currentQuestionIndex];
                document.getElementById('current-idx').textContent = this.currentQuestionIndex + 1;
                document.getElementById('total-idx').textContent = this.data.length;
                document.getElementById('question-txt').textContent = q.question;
                const optionsBox = document.getElementById('options-box');
                optionsBox.innerHTML = '';
                q.options.forEach((opt, idx) => {
                    const btn = document.createElement('button');
                    btn.type = 'button'; btn.className = 'option-btn'; btn.textContent = opt;
                    btn.onclick = () => this.selectOption(idx);
                    optionsBox.appendChild(btn);
                });
                const nextBtn = document.getElementById('next-btn');
                nextBtn.disabled = true;
                nextBtn.textContent = this.currentQuestionIndex === this.data.length - 1 ? "Finish Quiz" : "Next Question";
            }
            selectOption(index) {
                this.userAnswers[this.currentQuestionIndex] = index;
                const options = document.getElementById('options-box').children;
                for (let i = 0; i < options.length; i++) options[i].classList.remove('selected');
                options[index].classList.add('selected');
                document.getElementById('next-btn').disabled = false;
            }
            nextQuestion() {
                if (this.currentQuestionIndex < this.data.length - 1) { this.currentQuestionIndex++; this.loadQuestion(); }
                else { this.showResults(); }
            }
            showResults() {
                let score = 0; const reviewBox = document.getElementById('review-box'); reviewBox.innerHTML = '';
                this.data.forEach((item, idx) => {
                    const userSel = this.userAnswers[idx]; const isCorrect = userSel === item.correct;
                    if (isCorrect) score++;
                    const reviewItem = document.createElement('div'); reviewItem.className = 'review-item';
                    let htmlContent = `<div class="review-question">${"$"}{idx + 1}. ${"$"}{item.question}</div>`;
                    if (!isCorrect) {
                        htmlContent += `<div class="review-answer user-wrong"><strong>Your Answer:</strong> ${"$"}{item.options[userSel]}</div>`;
                    }
                    htmlContent += `<div class="review-answer correct-assigned"><strong>Correct Answer:</strong> ${"$"}{item.options[item.correct]}</div>
                        <div class="explanation-box"><span class="explanation-title">Explanation</span>${"$"}{item.explanation}</div>`;
                    reviewItem.innerHTML = htmlContent; reviewBox.appendChild(reviewItem);
                });
                document.getElementById('score-txt').textContent = score;
                document.getElementById('total-score-txt').textContent = this.data.length;
                this.switchScreen('result-screen');
            }
            resetQuiz() { this.currentQuestionIndex = 0; this.userAnswers = []; this.loadQuestion(); this.switchScreen('quiz-screen'); }
        }
        // //INIT_PLACEHOLDER//
        const quizApp = new QuizEngine(quizData); quizApp.init();
    </script>
</body>
</html>"""
        
        val fcTemplate = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Notebook Flashcards</title>
    <style>
        :root {
            --bg-color: #f4f1ea;
            --paper-color: #fcfbfa;
            --rule-color: #e0dad0;
            --margin-line: #ff7073;
            --text-color: #2b2b2b;
            --text-light: #666;
            --primary: #2e5cb8;
            --success: #2e7d32;
            --error: #c62828;
            --border-radius: 16px;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
        body {
            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
            background-color: var(--bg-color); color: var(--text-color);
            line-height: 1.6; padding: 12px; display: flex; justify-content: center; align-items: flex-start;
            min-height: 100vh; overflow-x: hidden;
        }
        .notebook {
            width: 100%; max-width: 600px; background-color: var(--paper-color);
            border-radius: var(--border-radius); box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
            border: 1px solid #dfdcd3; position: relative; min-height: 85vh; display: flex; flex-direction: column; overflow: hidden;
        }
        .notebook::before {
            content: ''; position: absolute; top: 0; left: 0; width: 8px; height: 100%;
            background: linear-gradient(to right, #333 30%, #555 70%, #222 100%); z-index: 10;
        }
        .paper-content {
            padding: 24px 16px 24px 36px;
            background-image: linear-gradient(var(--rule-color) 1px, transparent 1px);
            background-size: 100% 28px; position: relative; flex-grow: 1; display: flex; flex-direction: column;
        }
        .paper-content::before {
            content: ''; position: absolute; top: 0; left: 26px; width: 2px; height: 100%; background-color: var(--margin-line); opacity: 0.8;
        }
        h1, h2, h3 { color: var(--text-color); margin-bottom: 16px; font-weight: 600; }
        .screen { display: none; flex-direction: column; height: 100%; flex-grow: 1; animation: fadeIn 0.3s ease-in-out; }
        .screen.active { display: flex; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
        .progress { font-size: 0.9rem; font-weight: 600; color: var(--primary); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 20px; }
        .flashcard-container { perspective: 1000px; width: 100%; height: 320px; margin-bottom: 24px; cursor: pointer; }
        .flashcard { width: 100%; height: 100%; position: relative; transform-style: preserve-3d; transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1); }
        .flashcard.flipped { transform: rotateY(180deg); }
        .card-face {
            position: absolute; width: 100%; height: 100%; backface-visibility: hidden; border-radius: var(--border-radius);
            padding: 24px; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center;
            border: 2px solid #ccc7b9; background-color: #fff; box-shadow: 0 8px 16px rgba(0,0,0,0.06);
        }
        .card-front { color: var(--text-color); }
        .card-back { transform: rotateY(180deg); background-color: #f0f7ff; border-color: #bcd0eb; }
        .card-label { font-size: 0.8rem; font-weight: 700; text-transform: uppercase; letter-spacing: 1.5px; color: var(--primary); margin-bottom: 16px; position: absolute; top: 20px; }
        .card-text { font-size: 1.35rem; font-weight: 600; line-height: 1.4; }
        .tap-hint { font-size: 0.85rem; font-weight: 500; color: #888; text-transform: uppercase; letter-spacing: 1px; position: absolute; bottom: 20px; }
        .btn-container { margin-top: auto; padding-top: 20px; padding-bottom: 10px; }
        .action-row { display: flex; gap: 16px; width: 100%; }
        .primary-btn {
            flex: 1; color: white; border: none; padding: 16px; font-size: 1.05rem; font-weight: 600; border-radius: 8px; cursor: pointer;
            text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); transition: transform 0.1s; min-height: 56px;
        }
        .btn-wrong { background-color: var(--error); }
        .btn-right { background-color: var(--success); }
        .primary-btn:active { transform: translateY(2px); box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .score-display { text-align: center; margin: 30px 0; background: #fff; padding: 20px; border-radius: 12px; border: 1px solid #eee; }
        .score-number { font-size: 4rem; font-weight: bold; color: var(--primary); }
        .review-container { display: flex; flex-direction: column; gap: 20px; margin-top: 16px; }
        .review-item { background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #e0dad0; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
        .review-question { font-weight: 600; margin-bottom: 12px; font-size: 1.1rem; }
        .review-status { padding: 8px 16px; border-radius: 6px; margin-bottom: 12px; font-size: 0.9rem; font-weight: 600; display: inline-block; }
        .review-status.status-wrong { background-color: #ffebee; color: #c62828; border: 1px solid #ffcdd2; }
        .review-status.status-right { background-color: #e8f5e9; color: #2e7d32; border: 1px solid #c8e6c9; }
        .explanation-box { background-color: #f8f9fa; border: 1px solid #e9ecef; padding: 14px; border-radius: 6px; font-size: 0.95rem; color: #495057; }
        .explanation-title { font-weight: 700; margin-bottom: 6px; display: block; color: #212529; text-transform: uppercase; font-size: 0.8rem; letter-spacing: 0.5px; }
    </style>
</head>
<body>
    <div class="notebook">
        <div class="paper-content">
            <div id="flashcard-screen" class="screen active">
                <div class="progress">Card <span id="current-idx">1</span> / <span id="total-idx">3</span></div>
                <div class="flashcard-container" onclick="flashcardApp.flipCard()">
                    <div class="flashcard" id="card-element">
                        <div class="card-face card-front">
                            <span class="card-label">Question</span>
                            <div class="card-text" id="front-txt">Loading...</div>
                            <span class="tap-hint">Tap to flip</span>
                        </div>
                        <div class="card-face card-back">
                            <span class="card-label">Answer</span>
                            <div class="card-text" id="back-txt">Loading...</div>
                            <span class="tap-hint">Tap to flip</span>
                        </div>
                    </div>
                </div>
                <div class="btn-container">
                    <div id="instruction-prompt" style="text-align:center; color: var(--text-light); font-size:0.95rem; margin-bottom:16px; font-weight: 500;">
                        Tap the card to reveal the answer.
                    </div>
                    <div class="action-row" id="action-controls" style="display: none;">
                        <button class="primary-btn btn-wrong" onclick="flashcardApp.gradeCard(false)">Forgot</button>
                        <button class="primary-btn btn-right" onclick="flashcardApp.gradeCard(true)">Got It</button>
                    </div>
                </div>
            </div>
            <div id="result-screen" class="screen">
                <h2>Session Complete</h2>
                <div class="score-display">
                    <p style="color: var(--text-light); font-weight: 600; text-transform: uppercase; letter-spacing: 1px; font-size: 0.9rem;">Cards Mastered</p>
                    <div class="score-number"><span id="score-txt">0</span>/<span id="total-score-txt">0</span></div>
                </div>
                <h3>Review</h3>
                <div class="review-container" id="review-box"></div>
                <div class="btn-container" style="margin-top: 20px;">
                    <button class="primary-btn" onclick="flashcardApp.resetDeck()" style="background-color: var(--primary);">Restart Deck</button>
                </div>
            </div>
        </div>
    </div>
    <script>
        // //DECK_DATA_PLACEHOLDER//
        const deckData = [
            { question: "Q", answer: "A", explanation: "Exp" }
        ];

        class FlashcardEngine {
            constructor(data) { this.data = data; this.currentIndex = 0; this.scores = []; this.hasFlippedCurrent = false; }
            init() { this.loadCard(); }
            switchScreen(screenId) {
                document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
                document.getElementById(screenId).classList.add('active');
                window.scrollTo(0,0);
            }
            loadCard() {
                const item = this.data[this.currentIndex]; this.hasFlippedCurrent = false;
                document.getElementById('card-element').classList.remove('flipped');
                document.getElementById('action-controls').style.display = 'none';
                document.getElementById('instruction-prompt').style.display = 'block';
                document.getElementById('current-idx').textContent = this.currentIndex + 1;
                document.getElementById('total-idx').textContent = this.data.length;
                document.getElementById('front-txt').textContent = item.question;
                document.getElementById('back-txt').textContent = item.answer;
            }
            flipCard() {
                const card = document.getElementById('card-element'); card.classList.toggle('flipped');
                if (!this.hasFlippedCurrent) {
                    this.hasFlippedCurrent = true;
                    document.getElementById('action-controls').style.display = 'flex';
                    document.getElementById('instruction-prompt').style.display = 'none';
                }
            }
            gradeCard(knewIt) {
                this.scores[this.currentIndex] = knewIt;
                if (this.currentIndex < this.data.length - 1) { this.currentIndex++; this.loadCard(); }
                else { this.showSummary(); }
            }
            showSummary() {
                let correctCount = this.scores.filter(Boolean).length; const reviewBox = document.getElementById('review-box'); reviewBox.innerHTML = '';
                this.data.forEach((item, idx) => {
                    const markedCorrect = this.scores[idx]; const reviewItem = document.createElement('div'); reviewItem.className = 'review-item';
                    const statusClass = markedCorrect ? 'status-right' : 'status-wrong'; const statusText = markedCorrect ? 'Got It' : 'Forgot';
                    reviewItem.innerHTML = `<div class="review-question">${"$"}{idx + 1}. ${"$"}{item.question}</div>
                        <div class="review-status ${"$"}{statusClass}">${"$"}{statusText}</div>
                        <div class="explanation-box"><span class="explanation-title">Answer</span><strong>${"$"}{item.answer}</strong> — ${"$"}{item.explanation}</div>`;
                    reviewBox.appendChild(reviewItem);
                });
                document.getElementById('score-txt').textContent = correctCount;
                document.getElementById('total-score-txt').textContent = this.data.length;
                this.switchScreen('result-screen');
            }
            resetDeck() { this.currentIndex = 0; this.scores = []; this.loadCard(); this.switchScreen('flashcard-screen'); }
        }
        // //INIT_PLACEHOLDER//
        const flashcardApp = new FlashcardEngine(deckData); flashcardApp.init();
    </script>
</body>
</html>"""

        return """
            Read the following text and generate an interactive $quizType quiz on it with $difficultyLevel difficulty.
            Create exactly ${numberOfItems.ifEmpty { "5" }} comprehensive questions based on the provided content.
            
            Text to process:
            $contextText
            
            You must output ONLY a complete, functional, single standalone HTML page inside a markdown code block labeled as ```quiz ... ```
            
            IMPORTANT: Do not write the HTML from scratch! Use the following exact HTML template to ensure the design is perfect for our app.
            Your ONLY job is to take the template below, find the JavaScript data array `const quizData = [...]` or `const deckData = [...]`, and REPLACE that array with your generated questions.
            Do not change the CSS styles or the HTML structure.
            
            Here is the template you MUST use for "$quizType":
            
            ```html
            ${if (quizType.contains("Flashcard", ignoreCase = true)) fcTemplate else mcTemplate}
            ```
            
            Instructions for data format:
            If Multiple Choice, the array is `const quizData = [ { question: "...", options: ["A", "B", "C", "D"], correct: 1, explanation: "..." } ];`
            If Flashcards, the array is `const deckData = [ { question: "...", answer: "...", explanation: "..." } ];`
            
            Output ONLY the final merged HTML wrapped in ```quiz and ```. Do NOT include ANY conversational filler, introductory text, or ending remarks.
        """.trimIndent()
    }

    fun getTutorPrompt(currentNoteContent: String, userQuestion: String): String {
        return """
            We are running an interactive Study Quiz with the following content:
            $currentNoteContent
            
            The user has a question about this study material or quiz: "$userQuestion"
            
            Act as Papiro, the expert academic companion and tutor.
            Task 1: Enhance the user's question to make it clearer, more precise, and better aligned with the context.
            Task 2: Provide a helpful, clear, precise, and short, concise explanation answering this enhanced question. Use real-life analogies if needed as an example to make it easier to understand.
            
            Format your output EXACTLY like this:
            ### 🙋 Enhanced Question
            [Your enhanced version of the user's question]
            
            ### 🤖 AI Tutor Response
            [Your answer]
            
            Do NOT generate another quiz, table of contents, or conversational intros/outros. Simply output the enhanced question and the answer with NO other text.
        """.trimIndent()
    }
    
    fun getSummaryPrompt(summaryType: String): String {
        return "Read this document and create a $summaryType note out of it. Format with clear Markdown headings and bullet points. IMPORTANT: Output ONLY the summary note. Do NOT include ANY conversational filler, introductory text, or ending remarks."
    }
}
