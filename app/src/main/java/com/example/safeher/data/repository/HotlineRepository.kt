package com.example.safeher.data.repository

import com.example.safeher.ui.resource.Hotline
import com.example.safeher.ui.resource.HotlineCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HotlineRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val allHotlines = listOf(
        Hotline(
            "1", "Royal Malaysia Police (PDRM)", "999",
            "For immediate police assistance in emergencies, crime, or danger.",
            HotlineCategory.EMERGENCY
        ),
        Hotline("2", "Fire & Rescue Department (Bomba)", "994",
            "Call for fire, rescue, or hazardous emergency assistance.",
            HotlineCategory.EMERGENCY),
        Hotline("3", "Hospital / Ambulance Emergency", "999",
            "Call for urgent medical help or to request an ambulance.",
            HotlineCategory.MEDICAL),
        Hotline("4", "Mental Health Helpline (MIASA)", "1-800-820-066",
            "24/7 mental health support and emotional crisis counselling.",
            HotlineCategory.MENTAL_HEALTH),
        Hotline("5", "Befrienders Kuala Lumpur", "03-7627 2929",
            "Confidential emotional support for those feeling distressed, lonely, or suicidal.",
            HotlineCategory.MENTAL_HEALTH),
        Hotline("6", "Women's Aid Organisation (WAO)", "03-3000 8858",
            "Support, shelter, and counseling for women facing domestic violence.",
            HotlineCategory.SUPPORT),
        Hotline("7", "Talian Kasih (KPWKM)", "15999",
            "24-hour government helpline for family, welfare, and child protection issues.",
            HotlineCategory.SUPPORT),
        Hotline("8", "Childline Malaysia", "15999",
            "Dedicated child protection helpline for children in danger or distress.",
            HotlineCategory.SUPPORT),
        Hotline("9", "CyberSecurity Malaysia (MyCERT)", "1-300-88-2999",
            "Report cyberbullying, scams, data breaches, or online threats.",
            HotlineCategory.CYBER),
        Hotline("10", "National Anti-Drug Agency (AADK)", "03-8911 2233",
            "Assistance for drug-related problems, rehabilitation, and advice.",
            HotlineCategory.SUPPORT),
        Hotline("11", "JKM Talian Nur", "15999",
            "Support for victims of abuse, neglect, and social crisis via Jabatan Kebajikan Masyarakat.",
            HotlineCategory.SUPPORT),
        Hotline("12", "Malaysian Red Crescent Society", "03-4257 8726",
            "Emergency medical relief, disaster aid, and humanitarian assistance.",
            HotlineCategory.MEDICAL),
        Hotline("13", "MCMC Complaint Hotline", "1-800-188-030",
            "Report telecommunications or internet service issues, spam, and scams.",
            HotlineCategory.CYBER),
        Hotline("14", "Samaritans of Singapore (cross-border)", "+65 1767",
            "Emotional support hotline for those feeling hopeless or suicidal (available internationally).",
            HotlineCategory.MENTAL_HEALTH)
    )


    fun getHotlines(): Flow<List<Hotline>> = flow {
        emit(allHotlines)
    }

    private fun getUserFavoritesCollection() = auth.currentUser?.uid?.let { userId ->
        firestore.collection("users").document(userId).collection("favorites")
    }

    suspend fun getFavoriteHotlines(): Set<String> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = getUserFavoritesCollection()?.get()?.await()
                snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
            } catch (e: Exception) {
                println("Error fetching favorite hotlines: ${e.message}")
                emptySet()
            }
        }
    }

    suspend fun addFavorite(hotlineId: String) {
        withContext(Dispatchers.IO) {
            try {
                val favoriteData = mapOf("favoritedAt" to System.currentTimeMillis())
                getUserFavoritesCollection()?.document(hotlineId)?.set(favoriteData)?.await()
            } catch (e: Exception) {
                println("Error adding favorite: ${e.message}")
            }
        }
    }

    suspend fun removeFavorite(hotlineId: String) {
        withContext(Dispatchers.IO) {
            try {
                getUserFavoritesCollection()?.document(hotlineId)?.delete()?.await()
            } catch (e: Exception) {
                println("Error removing favorite: ${e.message}")
            }
        }
    }
}