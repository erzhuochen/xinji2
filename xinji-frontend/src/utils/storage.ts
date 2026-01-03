// src/utils/storage.ts
const STORAGE_KEY = 'xinji_data'

export interface DiaryEntry {
  id: string
  content: string
  mood: number
  tags?: string[]
  createdAt: string
}

export const saveToStorage = (data: DiaryEntry[]) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export const loadFromStorage = (): DiaryEntry[] => {
  const data = localStorage.getItem(STORAGE_KEY)
  return data ? JSON.parse(data) : []
}
