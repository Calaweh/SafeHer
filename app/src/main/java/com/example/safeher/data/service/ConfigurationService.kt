package com.example.safeher.data.service

interface ConfigurationService
{
    suspend fun fetchConfiguration(): Boolean
}