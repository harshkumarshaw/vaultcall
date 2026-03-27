package com.vaultcall.data.db

import androidx.room.TypeConverter
import com.vaultcall.data.model.CallType
import com.vaultcall.data.model.RuleAction
import com.vaultcall.data.model.RuleType

/**
 * Room type converters for enum types used in entities.
 *
 * Converts enum values to/from their String name representation
 * for database storage.
 */
class Converters {

    @TypeConverter
    fun fromRuleType(value: RuleType): String = value.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)

    @TypeConverter
    fun fromRuleAction(value: RuleAction): String = value.name

    @TypeConverter
    fun toRuleAction(value: String): RuleAction = RuleAction.valueOf(value)

    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = CallType.valueOf(value)
}
