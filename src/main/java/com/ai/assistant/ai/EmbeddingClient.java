package com.ai.assistant.ai;

import java.io.IOException;
import java.util.List;

/**
 * Abstracție peste furnizorul de embeddings. Claude nu are API de embeddings,
 * deci folosim Voyage; interfața izolează providerul ca să fie ușor de schimbat.
 */
public interface EmbeddingClient {

    /** Returnează vectorul de embedding pentru textul dat. */
    List<Float> embed(String text) throws IOException;

    /** Dimensiunea vectorilor produși (trebuie să fie egală cu dimensiunea indexului Pinecone). */
    int dimension();
}
