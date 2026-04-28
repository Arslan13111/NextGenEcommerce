package com.example.nextgenecommerce.data.local.dao

import androidx.room.*
import com.example.nextgenecommerce.data.models.AddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {

    @Query("SELECT * FROM addresses WHERE userId = :userId ORDER BY isDefault DESC, createdAt DESC")
    fun getUserAddresses(userId: String): Flow<List<AddressEntity>>

    @Query("SELECT * FROM addresses WHERE id = :addressId")
    fun getAddressById(addressId: String): Flow<AddressEntity?>

    @Query("SELECT * FROM addresses WHERE userId = :userId AND isDefault = 1 LIMIT 1")
    fun getDefaultAddress(userId: String): Flow<AddressEntity?>

    @Query("SELECT COUNT(*) FROM addresses WHERE userId = :userId")
    fun getAddressCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddresses(addresses: List<AddressEntity>)

    @Update
    suspend fun updateAddress(address: AddressEntity)

    @Delete
    suspend fun deleteAddress(address: AddressEntity)

    @Query("UPDATE addresses SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaultFlags(userId: String)

    @Query("UPDATE addresses SET isDefault = 1 WHERE id = :addressId")
    suspend fun setAddressAsDefault(addressId: String)

    @Query("DELETE FROM addresses WHERE userId = :userId")
    suspend fun deleteAllUserAddresses(userId: String)

    @Query("DELETE FROM addresses")
    suspend fun deleteAllAddresses()
}
