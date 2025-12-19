import request from './request'
import type { ApiResponse, PageResponse, Order, WechatPrepay } from '@/types'

/**
 * 创建订单
 */
export const createOrder = (planType: string, autoRenew?: boolean) => {
  return request.post<ApiResponse<Order>>('/order/create', { planType, autoRenew }).then(res => res.data)
}

/**
 * 获取订单列表
 */
export const getOrderList = (params: {
  page?: number
  pageSize?: number
  status?: string
}) => {
  return request.get<ApiResponse<PageResponse<Order>>>('/order/list', { params }).then(res => res.data)
}

/**
 * 获取订单详情
 */
export const getOrderDetail = (orderId: string) => {
  return request.get<ApiResponse<Order>>(`/order/${orderId}`).then(res => res.data)
}

/**
 * 取消订单
 */
export const cancelOrder = (orderId: string) => {
  return request.post<ApiResponse<void>>(`/order/${orderId}/cancel`).then(res => res.data)
}

/**
 * 微信支付预下单
 */
export const wechatPrepay = (orderId: string) => {
  return request.post<ApiResponse<WechatPrepay>>('/payment/wechat/prepay', { orderId }).then(res => res.data)
}

/**
 * 查询支付状态
 */
export const queryPaymentStatus = (orderId: string) => {
  return request.get<ApiResponse<Order>>(`/payment/order/${orderId}/status`).then(res => res.data)
}
