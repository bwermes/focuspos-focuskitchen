package com.amorphik.focuskitchen

class ApiService {
    var credentials = DeviceCredentials

    fun getChecks(businessDate: String): List<CheckDto>{
        return Networking.fetchJson("$credentials.ba")
    }
}