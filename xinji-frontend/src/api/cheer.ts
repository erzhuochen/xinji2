import request from './request'

/**
 * 获取随机一条加油语句
 */
export const getRandomQuote = () => {
  return request.get('/cheer-quotes/random')
}

/**
 * 添加加油语句
 * @param content 语句内容
 */
export const addQuote = (content: string) => {
  return request.post('/cheer-quotes', { content })
}





