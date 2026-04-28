package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.AddressDao
import com.example.nextgenecommerce.data.models.Address
import com.example.nextgenecommerce.data.models.AddressEntity
import com.example.nextgenecommerce.data.models.toAddress
import com.example.nextgenecommerce.data.models.toEntity
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Supabase insert/update payload — no timestamps (Postgres handles them via DEFAULT now())
@Serializable
private data class AddressPayload(
    @SerialName("id")           val id: String,
    @SerialName("user_id")      val userId: String,
    @SerialName("label")        val label: String,
    @SerialName("full_name")    val fullName: String,
    @SerialName("phone")        val phone: String,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String,
    @SerialName("city")         val city: String,
    @SerialName("province")     val province: String,
    @SerialName("postal_code")  val postalCode: String,
    @SerialName("country")      val country: String,
    @SerialName("is_default")   val isDefault: Boolean
)

private fun AddressEntity.toPayload() = AddressPayload(
    id = id, userId = userId, label = label, fullName = fullName,
    phone = phone, addressLine1 = addressLine1, addressLine2 = addressLine2,
    city = city, province = province, postalCode = postalCode,
    country = country, isDefault = isDefault
)

@Singleton
class AddressRepository @Inject constructor(
    private val addressDao: AddressDao,
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
) {

    fun getUserAddresses(userId: String): Flow<List<Address>> =
        addressDao.getUserAddresses(userId).map { entities -> entities.map { it.toAddress() } }

    fun getAddressById(addressId: String): Flow<Address?> =
        addressDao.getAddressById(addressId).map { it?.toAddress() }

    fun getDefaultAddress(userId: String): Flow<Address?> =
        addressDao.getDefaultAddress(userId).map { it?.toAddress() }

    suspend fun addAddress(address: Address): Flow<Resource<Address>> = flow {
        emit(Resource.Loading())
        try {
            val entity = address.toEntity().copy(
                id = if (address.id.isEmpty()) UUID.randomUUID().toString() else address.id
            )

            supabaseDb.from("shipping_addresses").insert(entity.toPayload())

            addressDao.insertAddress(entity)
            emit(Resource.Success(entity.toAddress()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add address"))
            // Fallback: save locally only
            try {
                val localEntity = address.toEntity().copy(
                    id = if (address.id.isEmpty()) UUID.randomUUID().toString() else address.id
                )
                addressDao.insertAddress(localEntity)
                emit(Resource.Success(localEntity.toAddress()))
            } catch (localError: Exception) {
                emit(Resource.Error("Failed to save address: ${localError.message}"))
            }
        }
    }

    suspend fun updateAddress(address: Address): Flow<Resource<Address>> = flow {
        emit(Resource.Loading())
        try {
            val entity = address.toEntity()

            supabaseDb.from("shipping_addresses")
                .update(entity.toPayload()) {
                    filter { eq("id", address.id) }
                }

            addressDao.updateAddress(entity)
            emit(Resource.Success(address))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update address"))
            try {
                addressDao.updateAddress(address.toEntity())
                emit(Resource.Success(address))
            } catch (localError: Exception) {
                emit(Resource.Error("Failed to update address: ${localError.message}"))
            }
        }
    }

    suspend fun setDefaultAddress(addressId: String, userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Clear all defaults in Supabase
            supabaseDb.from("shipping_addresses")
                .update(mapOf("is_default" to false)) {
                    filter { eq("user_id", userId) }
                }
            // Set new default in Supabase
            supabaseDb.from("shipping_addresses")
                .update(mapOf("is_default" to true)) {
                    filter { eq("id", addressId) }
                }

            addressDao.clearDefaultFlags(userId)
            addressDao.setAddressAsDefault(addressId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            // Fallback to local only
            try {
                addressDao.clearDefaultFlags(userId)
                addressDao.setAddressAsDefault(addressId)
                emit(Resource.Success(Unit))
            } catch (localError: Exception) {
                emit(Resource.Error("Failed to set default: ${localError.message}"))
            }
        }
    }

    suspend fun deleteAddress(address: Address): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("shipping_addresses")
                .delete { filter { eq("id", address.id) } }

            addressDao.deleteAddress(address.toEntity())
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete address"))
            try {
                addressDao.deleteAddress(address.toEntity())
                emit(Resource.Success(Unit))
            } catch (localError: Exception) {
                emit(Resource.Error("Failed to delete address: ${localError.message}"))
            }
        }
    }

    suspend fun syncAddressesFromSupabase(userId: String): Flow<Resource<List<Address>>> = flow {
        emit(Resource.Loading())
        try {
            val addresses = supabaseDb.from("shipping_addresses")
                .select(Columns.ALL) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<AddressEntity>()

            addresses.forEach { addressDao.insertAddress(it) }
            emit(Resource.Success(addresses.map { it.toAddress() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to sync addresses"))
        }
    }
}
