import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DiaryItem, DiaryDetail } from '@/types'

export const useDiaryStore = defineStore('diary', () => {
  // 状态
  const diaryList = ref<DiaryItem[]>([])
  const currentDiary = ref<DiaryDetail | null>(null)
  const total = ref(0)
  const loading = ref(false)
  
  // 方法
  const setDiaryList = (list: DiaryItem[], totalCount: number) => {
    diaryList.value = list
    total.value = totalCount
  }
  
  const appendDiaryList = (list: DiaryItem[]) => {
    diaryList.value.push(...list)
  }
  
  const setCurrentDiary = (diary: DiaryDetail | null) => {
    currentDiary.value = diary
  }
  
  const updateDiaryInList = (id: string, updates: Partial<DiaryItem>) => {
    const index = diaryList.value.findIndex(d => d.id === id)
    if (index !== -1) {
      diaryList.value[index] = { ...diaryList.value[index], ...updates }
    }
  }
  
  const removeDiaryFromList = (id: string) => {
    const index = diaryList.value.findIndex(d => d.id === id)
    if (index !== -1) {
      diaryList.value.splice(index, 1)
      total.value--
    }
  }
  
  const clearDiaryList = () => {
    diaryList.value = []
    total.value = 0
  }
  
  return {
    // 状态
    diaryList,
    currentDiary,
    total,
    loading,
    // 方法
    setDiaryList,
    appendDiaryList,
    setCurrentDiary,
    updateDiaryInList,
    removeDiaryFromList,
    clearDiaryList
  }
})
