import { Router } from 'express';
import { ingestText, askAi } from '../controllers/rag.controller'; // Import từ file controller vừa tạo

const router = Router();

// Định nghĩa endpoint
router.post('/ingest', ingestText);
router.post('/ask', askAi);

export default router;