// ============================================
// 心迹 (XinJi) MongoDB 初始化脚本
// MongoDB 6.0+
// 执行方式: mongosh < init-mongo.js
// ============================================

// 切换到xinji数据库
use xinji;

// ============================================
// 创建集合
// ============================================

// 日记内容集合
db.createCollection("diary_contents", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["diaryId", "userId", "content"],
            properties: {
                diaryId: {
                    bsonType: "string",
                    description: "关联MySQL日记ID"
                },
                userId: {
                    bsonType: "string",
                    description: "用户ID"
                },
                content: {
                    bsonType: "string",
                    description: "日记正文(AES加密)"
                },
                preview: {
                    bsonType: "string",
                    description: "内容预览(前100字)"
                },
                wordCount: {
                    bsonType: "int",
                    description: "字数统计"
                },
                createdAt: {
                    bsonType: "date",
                    description: "创建时间"
                },
                updatedAt: {
                    bsonType: "date",
                    description: "更新时间"
                }
            }
        }
    }
});

// 分析结果集合
db.createCollection("analysis_results", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["diaryId", "userId", "status"],
            properties: {
                diaryId: {
                    bsonType: "string",
                    description: "关联MySQL日记ID"
                },
                userId: {
                    bsonType: "string",
                    description: "用户ID"
                },
                status: {
                    enum: ["PROCESSING", "COMPLETED", "FAILED"],
                    description: "分析状态"
                },
                emotions: {
                    bsonType: "object",
                    description: "情绪分布 {emotion: score}"
                },
                primaryEmotion: {
                    bsonType: "string",
                    description: "主要情绪"
                },
                emotionIntensity: {
                    bsonType: "double",
                    description: "情绪强度(0-1)"
                },
                keywords: {
                    bsonType: "array",
                    description: "关键词列表"
                },
                cognitiveDistortions: {
                    bsonType: "array",
                    description: "认知扭曲 [{type, description}]"
                },
                suggestions: {
                    bsonType: "array",
                    description: "心理调适建议"
                },
                riskLevel: {
                    enum: ["LOW", "MEDIUM", "HIGH", null],
                    description: "风险等级"
                },
                rawResponse: {
                    bsonType: "string",
                    description: "AI原始响应"
                },
                analyzedAt: {
                    bsonType: "date",
                    description: "分析完成时间"
                },
                createdAt: {
                    bsonType: "date",
                    description: "创建时间"
                }
            }
        }
    }
});

// 周报集合
db.createCollection("weekly_reports", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "weekStart", "weekEnd"],
            properties: {
                userId: {
                    bsonType: "string",
                    description: "用户ID"
                },
                weekStart: {
                    bsonType: "date",
                    description: "周开始日期"
                },
                weekEnd: {
                    bsonType: "date",
                    description: "周结束日期"
                },
                diaryCount: {
                    bsonType: "int",
                    description: "日记数量"
                },
                analyzedCount: {
                    bsonType: "int",
                    description: "已分析数量"
                },
                emotionTrend: {
                    bsonType: "array",
                    description: "情绪趋势 [{date, emotion, intensity}]"
                },
                emotionDistribution: {
                    bsonType: "object",
                    description: "情绪分布统计 {emotion: count}"
                },
                averageIntensity: {
                    bsonType: "double",
                    description: "平均情绪强度"
                },
                mostFrequentEmotion: {
                    bsonType: "string",
                    description: "最频繁情绪"
                },
                keywords: {
                    bsonType: "array",
                    description: "本周关键词"
                },
                summary: {
                    bsonType: "string",
                    description: "AI周总结"
                },
                createdAt: {
                    bsonType: "date",
                    description: "创建时间"
                }
            }
        }
    }
});

// ============================================
// 创建索引
// ============================================

// 日记内容索引
db.diary_contents.createIndex({ "diaryId": 1 }, { unique: true });
db.diary_contents.createIndex({ "userId": 1 });
db.diary_contents.createIndex({ "createdAt": -1 });

// 分析结果索引
db.analysis_results.createIndex({ "diaryId": 1 }, { unique: true });
db.analysis_results.createIndex({ "userId": 1 });
db.analysis_results.createIndex({ "status": 1 });
db.analysis_results.createIndex({ "userId": 1, "createdAt": -1 });

// 周报索引
db.weekly_reports.createIndex({ "userId": 1, "weekStart": 1 }, { unique: true });
db.weekly_reports.createIndex({ "userId": 1, "createdAt": -1 });

// ============================================
// 显示创建结果
// ============================================
print("============================================");
print("心迹 MongoDB 初始化完成!");
print("============================================");
print("集合列表:");
db.getCollectionNames().forEach(function(c) {
    print("  - " + c);
});
print("============================================");
