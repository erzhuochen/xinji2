/**
 * 本地存储工具函数
 */

/**
 * 保存数据到 localStorage
 */
export function saveToStorage(key: string, value: any): void {
  try {
    const serialized = JSON.stringify(value)
    localStorage.setItem(key, serialized)
  } catch (error) {
    console.error('保存到 localStorage 失败:', error)
  }
}

/**
 * 从 localStorage 读取数据
 */
export function getFromStorage<T>(key: string): T | null {
  try {
    const item = localStorage.getItem(key)
    if (item === null) return null
    return JSON.parse(item) as T
  } catch (error) {
    console.error('从 localStorage 读取失败:', error)
    return null
  }
}

/**
 * 从 localStorage 删除数据
 */
export function removeFromStorage(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch (error) {
    console.error('从 localStorage 删除失败:', error)
  }
}








