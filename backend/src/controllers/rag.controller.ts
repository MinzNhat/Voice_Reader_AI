import { Request, Response } from 'express';
import { Pinecone } from '@pinecone-database/pinecone';
import { PineconeStore } from '@langchain/pinecone';
import { ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings } from '@langchain/google-genai';
import { RetrievalQAChain } from 'langchain/chains';
import { RecursiveCharacterTextSplitter } from 'langchain/text_splitter';
import { TaskType } from '@google/generative-ai';

// --- CONFIG ---
const pinecone = new Pinecone({
    apiKey: process.env.PINECONE_API_KEY!
});
const pineconeIndex = pinecone.Index(process.env.PINECONE_INDEX_NAME!);

const embeddings = new GoogleGenerativeAIEmbeddings({
    model: "text-embedding-004",
    taskType: TaskType.RETRIEVAL_DOCUMENT,
    title: "Document",
    apiKey: process.env.GOOGLE_API_KEY
});

const llm = new ChatGoogleGenerativeAI({
    model: "gemini-2.0-flash",
    maxOutputTokens: 2048,
    apiKey: process.env.GOOGLE_API_KEY
});

// --- LOGIC FUNCTIONS ---

export const ingestText = async (req: Request, res: Response) => {
    try {
        console.log("🧠 Ingesting...");
        const { text } = req.body;
        if (!text) {
             res.status(400).json({ success: false, error: "Missing text" });
             return;
        }

        const splitter = new RecursiveCharacterTextSplitter({ chunkSize: 500, chunkOverlap: 50 });
        const docs = await splitter.createDocuments([text]);
        
        await PineconeStore.fromDocuments(docs, embeddings, { pineconeIndex });
        
        console.log("✅ Ingest Success");
        res.json({ success: true, message: "AI learned successfully" });
    } catch (error: any) {
        console.error("Ingest Error:", error);
        res.status(500).json({ success: false, error: error.message });
    }
};

export const askAi = async (req: Request, res: Response) => {
    try {
        console.log("❓ Asking...");
        const { question } = req.body;
        if (!question) {
            res.status(400).json({ success: false, error: "Missing question" });
            return;
        }

        const vectorStore = await PineconeStore.fromExistingIndex(embeddings, { pineconeIndex });
        
        // Chain xử lý (Bạn có thể thêm PromptTemplate vào đây nếu muốn)
        const chain = RetrievalQAChain.fromLLM(llm, vectorStore.asRetriever({ k: 3 }), { 
            returnSourceDocuments: true 
        });

        const response = await chain.call({ query: question });
        console.log("✅ Answered");
        
        res.json({ 
            success: true,
            answer: response.text, 
            source: response.sourceDocuments 
        });
    } catch (error: any) {
        console.error("Ask Error:", error);
        res.status(500).json({ success: false, error: error.message });
    }
};