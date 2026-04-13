package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.OrderDao
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.remote.ApiService
import com.example.nextgenecommerce.data.remote.CreateOrderRequest
import com.example.nextgenecommerce.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val apiService: ApiService,
    private val orderDao: OrderDao
) {

    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()

    fun getOrderById(orderId: String): Flow<Order?> = orderDao.getOrderById(orderId)

    fun getUserOrders(userId: String): Flow<List<Order>> = orderDao.getOrdersByUserId(userId)

    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> = orderDao.getOrdersByStatus(status)

    suspend fun createOrder(request: CreateOrderRequest): Flow<Resource<Order>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.createOrder(request)
            if (response.isSuccessful && response.body()?.order != null) {
                val order = response.body()!!.order!!
                orderDao.insertOrder(order)
                emit(Resource.Success(order))
            } else {
                emit(Resource.Error("Failed to create order"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    suspend fun insertOrder(order: Order) {
        orderDao.insertOrder(order)
    }

    suspend fun updateOrder(order: Order) {
        orderDao.updateOrder(order)
    }

    suspend fun syncOrders(userId: String): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserOrders(userId)
            if (response.isSuccessful && response.body() != null) {
                val orders = response.body()!!.orders
                orders.forEach { orderDao.insertOrder(it) }
                emit(Resource.Success(orders))
            } else {
                emit(Resource.Error("Failed to fetch orders"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }
}
