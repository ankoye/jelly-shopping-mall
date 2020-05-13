import request from '@/common/utils/request'

export function getDateMenu() {
  return request({
    url: 'sApi/v1/seckill/goods/menus',
    method: 'get'
  })
}

export function getGoodsList(menu: string) {
  return request({
    url: `sApi/v1/seckill/goods/time_list/${menu}`,
    method: 'get'
  })
}

export function getGoods(menu: string, spuId: string) {
  return request({
    url: `sApi/v1/seckill/goods/${menu}/${spuId}`,
    method: 'get'
  })
}

export function queueUp(data: any) {
  return request({
    url: 'sApi/v1/seckill/order/queueUp',
    method: 'post',
    data: JSON.stringify(data)
  })
}

export function queryQueue(userId: string, goodsId: string) {
  return request({
    url: `sApi/v1/seckill/order/queue/${userId}/${goodsId}`,
    method: 'get'
  })
}
