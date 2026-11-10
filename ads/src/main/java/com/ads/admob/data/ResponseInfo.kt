package com.ads.admob.data

data class AdapterResponseInfo(
    val adSourceName: String = "",
    val adSourceId: String = "",
    val adSourceInstanceName: String = "",
    val adSourceInstanceId: String = "",
    val mediationAdapterClassName: String = "",
    val latencyMillis: Long = 0L,
    val adError: String? = null,
    val credentials: Map<String, String> = emptyMap()
)

data class ResponseInfo(
    val responseId: String? = null,
    val mediationAdapterClassName: String? = null,
    val adapterResponses: List<AdapterResponseInfo> = emptyList(),
    val loadedAdapterResponse: AdapterResponseInfo? = null,
    val responseExtras: Map<String, String> = emptyMap(),
    val adNetworkName: String = loadedAdapterResponse?.adSourceName ?: "",
    val latencyMillis: Long = loadedAdapterResponse?.latencyMillis ?: 0L
) {
    val responseID: String? get() = responseId

    companion object {
        fun fromGmsResponseInfo(gmsInfo: com.google.android.gms.ads.ResponseInfo?): ResponseInfo? {
            if (gmsInfo == null) return null
            val adapterResponses = gmsInfo.adapterResponses.map { adapter ->
                AdapterResponseInfo(
                    adSourceName = adapter.adSourceName,
                    adSourceId = adapter.adSourceId,
                    adSourceInstanceName = adapter.adSourceInstanceName,
                    adSourceInstanceId = adapter.adSourceInstanceId,
                    mediationAdapterClassName = adapter.adapterClassName,
                    latencyMillis = adapter.latencyMillis,
                    adError = adapter.adError?.message
                )
            }
            val loadedAdapter = gmsInfo.loadedAdapterResponseInfo?.let { adapter ->
                AdapterResponseInfo(
                    adSourceName = adapter.adSourceName,
                    adSourceId = adapter.adSourceId,
                    adSourceInstanceName = adapter.adSourceInstanceName,
                    adSourceInstanceId = adapter.adSourceInstanceId,
                    mediationAdapterClassName = adapter.adapterClassName,
                    latencyMillis = adapter.latencyMillis,
                    adError = adapter.adError?.message
                )
            }
            return ResponseInfo(
                responseId = gmsInfo.responseId,
                mediationAdapterClassName = gmsInfo.mediationAdapterClassName,
                adapterResponses = adapterResponses,
                loadedAdapterResponse = loadedAdapter,
                adNetworkName = loadedAdapter?.adSourceName ?: gmsInfo.mediationAdapterClassName ?: "",
                latencyMillis = loadedAdapter?.latencyMillis ?: 0L
            )
        }
    }
}
