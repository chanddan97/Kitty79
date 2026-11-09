package com.example.data.model

enum class ModelStatus {
    UNKNOWN,
    AVAILABLE,
    TESTING,
    ERROR,
    RATE_LIMITED
}

data class GeminiModelConfig(
    val id: String,
    val displayName: String,
    val description: String,
    val isLegacy: Boolean = false,
    val isCustom: Boolean = false,
    val status: ModelStatus = ModelStatus.UNKNOWN,
    val statusMessage: String? = null
)

data class VoiceOption(
    val name: String,
    val gender: String,
    val description: String,
    val previewText: String
)

enum class TtsLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val sampleText: String
) {
    AUTO("auto", "Auto Detect", "স্বয়ংক্রিয় / स्वचालित", "স্বয়ংক্রিয় সনাক্তকরণ"),
    BENGALI("bn", "Bengali", "বাংলা", "নমস্কার! কৃটি এআই ভয়েস জেনারেটরে আপনাকে স্বাগতম।"),
    HINDI("hi", "Hindi", "हिन्दी", "नमस्ते! कृती एআই वॉइस जनरेटर में आपका स्वागत है।"),
    ENGLISH("en", "English", "English", "Welcome to KRITY AI Voice Generator. Natural speech synthesis powered by Gemini.")
}

data class TtsScene(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val description: String,
    val aiContextPrompt: String,
    val recommendedStyle: SpeakingStyle = SpeakingStyle.NATURAL,
    val recommendedEmotion: VoiceEmotion = VoiceEmotion.NEUTRAL
)

enum class SpeakingStyle(val displayName: String, val instruction: String) {
    NATURAL("Natural", "Speak naturally and fluidly with standard conversational rhythm."),
    STORYTELLING("Storytelling", "Speak like an engaging, captivating storyteller with expressive narrative cadence and natural dramatic pauses."),
    FRIENDLY("Friendly", "Speak in a cheerful, warm, and inviting tone with friendly inflections."),
    PROFESSIONAL("Professional", "Speak clearly, authoritatively, and articulate each word with business-standard pronunciation."),
    NEWS("News", "Speak with the crisp, objective, steady pace of a broadcast news anchor."),
    CALM("Calm", "Speak in a gentle, tranquil, relaxed, and soothing voice."),
    EXCITED("Excited", "Speak with high energy, enthusiasm, and dynamic vocal brightness."),
    EMOTIONAL("Emotional", "Speak with deep emotional resonance, heartfelt nuance, and sincerity."),
    DRAMATIC("Dramatic", "Speak theatrically with rich emphasis, suspenseful pauses, and dramatic flair."),
    WHISPER("Whisper", "Speak softly in a quiet, intimate, near-whispering tone.")
}

enum class VoiceEmotion(val displayName: String, val promptDescriptor: String) {
    NEUTRAL("Neutral", "neutral emotional baseline"),
    HAPPY("Happy", "joyful, cheerful, and upbeat"),
    SAD("Sad", "melancholy, subdued, and poignant"),
    ANGRY("Angry", "intense, firm, and displeased"),
    EXCITED("Excited", "electrified, elated, and vibrant"),
    CALM("Calm", "peaceful, serene, and steady"),
    SERIOUS("Serious", "grave, earnest, and disciplined"),
    WARM("Warm", "affectionate, gentle, and comforting"),
    DRAMATIC("Dramatic", "passionate, vivid, and powerful")
}

object DefaultModelPresets {
    val scenes = listOf(
        TtsScene(
            id = "general",
            nameEn = "General / Conversational",
            nameBn = "সাধারণ কথোপকথন",
            description = "Natural everyday dialogue with smooth pacing.",
            aiContextPrompt = "You are delivering natural speech. Speak fluidly, casually, with normal conversational pauses and authentic intonation.",
            recommendedStyle = SpeakingStyle.NATURAL,
            recommendedEmotion = VoiceEmotion.NEUTRAL
        ),
        TtsScene(
            id = "youtube_narration",
            nameEn = "YouTube & Documentary",
            nameBn = "ইউটিউব ও তথ্যচিত্র",
            description = "Captivating, high-clarity voiceover that grips listener attention.",
            aiContextPrompt = "You are an expert YouTube documentary voiceover artist. Deliver the text with gripping curiosity, clear articulation, dynamic tonal shifts between suspense and explanation, and cinematic vocal presence.",
            recommendedStyle = SpeakingStyle.STORYTELLING,
            recommendedEmotion = VoiceEmotion.WARM
        ),
        TtsScene(
            id = "story_fairy",
            nameEn = "Bedtime & Storytelling",
            nameBn = "গল্প ও রূপকথা",
            description = "Gentle, magical, and warm storytelling for stories and kids.",
            aiContextPrompt = "You are a warm, loving storyteller reading an immersive story. Speak softly, with magical wonder, gentle cadence, comforting intimacy, and vivid dramatic expression for characters.",
            recommendedStyle = SpeakingStyle.STORYTELLING,
            recommendedEmotion = VoiceEmotion.WARM
        ),
        TtsScene(
            id = "news_broadcast",
            nameEn = "News & Headline Bulletin",
            nameBn = "সংবাদ বুলেটিন",
            description = "Authoritative, crisp, and objective broadcast journalism tone.",
            aiContextPrompt = "You are a national broadcast news anchor. Read with authoritative precision, crisp diction, objective gravitas, steady rhythm, and zero vocal hesitation.",
            recommendedStyle = SpeakingStyle.NEWS,
            recommendedEmotion = VoiceEmotion.SERIOUS
        ),
        TtsScene(
            id = "motivational",
            nameEn = "Motivational & Speech",
            nameBn = "অনুপ্রেরণামূলক বক্তব্য",
            description = "Inspiring, powerful delivery filled with passion and conviction.",
            aiContextPrompt = "You are an inspiring keynote motivational speaker. Speak with electric energy, heartfelt conviction, bold dramatic pauses, and uplifting vocal resonance to ignite passion in the listener.",
            recommendedStyle = SpeakingStyle.EXCITED,
            recommendedEmotion = VoiceEmotion.EXCITED
        ),
        TtsScene(
            id = "podcast_host",
            nameEn = "Podcast & Talk Show",
            nameBn = "পডকাস্ট ও আড্ডা",
            description = "Intimate, relatable mic presence with cozy coffee-shop warmth.",
            aiContextPrompt = "You are a charismatic, friendly podcast host speaking directly to a close friend. Use a warm, relaxed, conversational tone with authentic micro-inflections and approachable charisma.",
            recommendedStyle = SpeakingStyle.FRIENDLY,
            recommendedEmotion = VoiceEmotion.WARM
        ),
        TtsScene(
            id = "poetry_recitation",
            nameEn = "Poetry & Literature",
            nameBn = "কবিতা ও আবৃত্তি",
            description = "Soulful, artistic recitation with deep emotional sensitivity.",
            aiContextPrompt = "You are a master poet and literary reciter. Read with profound emotional depth, artistic reverence, gentle cadence, poignant pauses, and deep literary sensitivity.",
            recommendedStyle = SpeakingStyle.EMOTIONAL,
            recommendedEmotion = VoiceEmotion.DRAMATIC
        ),
        TtsScene(
            id = "audiobook",
            nameEn = "Audiobook Chapter",
            nameBn = "অডিওবুক পাঠ",
            description = "Long-form listening comfort with nuanced character pacing.",
            aiContextPrompt = "You are a seasoned audiobook narrator. Deliver steady, immersive, nuanced vocal distinction with clear enunciation and comfortable pacing optimized for long-form listening.",
            recommendedStyle = SpeakingStyle.STORYTELLING,
            recommendedEmotion = VoiceEmotion.CALM
        ),
        TtsScene(
            id = "commercial_ad",
            nameEn = "Ad & Commercial Promo",
            nameBn = "বিজ্ঞাপন ও প্রচার",
            description = "Punchy, persuasive, and energetic promotional delivery.",
            aiContextPrompt = "You are a top-tier commercial advertising voiceover artist. Deliver with upbeat confidence, punchy excitement, persuasive clarity, and enthusiastic call-to-action impact.",
            recommendedStyle = SpeakingStyle.EXCITED,
            recommendedEmotion = VoiceEmotion.HAPPY
        ),
        TtsScene(
            id = "tutorial_edu",
            nameEn = "E-Learning & Tutorial",
            nameBn = "টিউটোরিয়াল ও ক্লাস",
            description = "Patient, instructional teaching voice with pedagogical warmth.",
            aiContextPrompt = "You are an expert instructor teaching an educational lesson. Speak patiently, clearly, with pedagogical clarity, encouraging warmth, and deliberate emphasis on key concepts.",
            recommendedStyle = SpeakingStyle.PROFESSIONAL,
            recommendedEmotion = VoiceEmotion.CALM
        )
    )

    val models = listOf(
        GeminiModelConfig(
            id = "gemini-3.8-flash-tts",
            displayName = "Gemini 3.8 Flash TTS",
            description = "High-quality expressive speech with multi-lingual voice synthesis.",
            isLegacy = false
        ),
        GeminiModelConfig(
            id = "gemini-3.8-flash-lite-tts",
            displayName = "Gemini 3.8 Flash-Lite TTS",
            description = "Ultra-fast lightweight speech synthesis with minimal latency.",
            isLegacy = false
        ),
        GeminiModelConfig(
            id = "gemini-3.1-flash-tts",
            displayName = "Gemini 3.1 Flash TTS",
            description = "Legacy compatibility model for stable text-to-speech fallback.",
            isLegacy = true
        ),
        GeminiModelConfig(
            id = "gemini-2.5-flash-preview-tts",
            displayName = "Gemini 2.5 Flash TTS",
            description = "Reliable multimodal audio generation endpoint for Bengali, Hindi, and English.",
            isLegacy = false
        )
    )

    val voices = listOf(
        VoiceOption("Puck", "Male", "Playful & energetic", "নমস্কার! আমি পাক। আপনার দিনটি শুভ হোক।"),
        VoiceOption("Charon", "Male", "Calm & mature", "नमस्ते! मैं चारोन हूँ। शांत और स्पष्ट वाणी।"),
        VoiceOption("Kore", "Female", "Warm & soothing", "Hello! I am Kore. Natural and pleasant tone."),
        VoiceOption("Fenrir", "Male", "Deep & authoritative", "Welcome to KRITY AI voice generation."),
        VoiceOption("Aoede", "Female", "Melodic & expressive", "গল্প এবং কবিতার জন্য অপূর্ব ছন্দময় কণ্ঠ।"),
        VoiceOption("Zephyr", "Female", "Gentle & airy", "हल्की और मनमोहक आवाज़ में सुनिए।"),
        VoiceOption("Leda", "Female", "Youthful & bright", "Crisp, lively, and highly articulate speech."),
        VoiceOption("Orpheus", "Male", "Resonant & narrative", "দৃঢ় ও গভীর কণ্ঠস্বর।"),
        VoiceOption("Callisto", "Female", "Clear & professional", "Official news and business updates."),
        VoiceOption("Chiron", "Male", "Conversational & friendly", "Friendly guidance and daily conversation.")
    )

    val speeds = listOf(0.75f, 0.85f, 1.0f, 1.15f, 1.25f, 1.5f)
}
