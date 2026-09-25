package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MatchType
import com.example.data.model.ReplyRule
import com.example.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleDialog(
    initialRule: ReplyRule?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        pattern: String,
        matchType: MatchType,
        replyText: String,
        isGroupAllowed: Boolean,
        cooldownSeconds: Int,
        priority: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var pattern by remember { mutableStateOf(initialRule?.incomingPattern ?: "") }
    var matchType by remember { mutableStateOf(initialRule?.matchType ?: MatchType.CONTAINS) }
    var replyText by remember { mutableStateOf(initialRule?.replyText ?: "") }
    var isGroupAllowed by remember { mutableStateOf(initialRule?.isGroupAllowed ?: false) }
    var cooldownSeconds by remember { mutableIntStateOf(initialRule?.cooldownSeconds ?: 180) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var patternError by remember { mutableStateOf(false) }
    var replyError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("rule_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (initialRule == null) "New Auto-Reply Rule" else "Edit Rule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_rule_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rule Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Rule Name / Title") },
                    placeholder = { Text("e.g., General Greeting, Pricing Inquiry") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Rule name is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Match Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = matchType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Match Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("match_type_selector")
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        MatchType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = type.displayName, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = type.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    matchType = type
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pattern Input (Hidden if Fallback)
                if (matchType != MatchType.FALLBACK_DEFAULT) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = {
                            pattern = it
                            if (it.isNotBlank()) patternError = false
                        },
                        label = { Text("Incoming Message Pattern / Keywords") },
                        placeholder = {
                            Text(
                                when (matchType) {
                                    MatchType.EXACT -> "e.g., hi, hello, salam"
                                    MatchType.CONTAINS -> "e.g., price, cost, rate"
                                    MatchType.STARTS_WITH -> "e.g., help, info"
                                    MatchType.REGEX -> "e.g., (urgent|important|asap)"
                                    else -> ""
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                "Separate multiple trigger words with commas",
                                fontSize = 11.sp
                            )
                        },
                        isError = patternError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rule_pattern_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Reply Text Field
                OutlinedTextField(
                    value = replyText,
                    onValueChange = {
                        replyText = it
                        if (it.isNotBlank()) replyError = false
                    },
                    label = { Text("Auto-Reply Message") },
                    placeholder = { Text("Type the response message that will be sent back...") },
                    minLines = 3,
                    maxLines = 6,
                    isError = replyError,
                    supportingText = if (replyError) {
                        { Text("Reply message is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_reply_text_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Variable insertion tags
                Text(
                    text = "Insert dynamic placeholder:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { replyText += " {sender}" },
                        label = { Text("{sender}") }
                    )
                    AssistChip(
                        onClick = { replyText += " {time}" },
                        label = { Text("{time}") }
                    )
                    AssistChip(
                        onClick = { replyText += " {date}" },
                        label = { Text("{date}") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Group chats toggle
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reply in WhatsApp Groups",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Allow this specific rule to reply to group messages",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isGroupAllowed,
                            onCheckedChange = { isGroupAllowed = it },
                            modifier = Modifier.testTag("rule_group_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cooldown Setting
                Text(
                    text = "Sender Cooldown Interval:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Don't send this auto-reply to the same contact multiple times within:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val cooldownOptions = listOf(
                    0 to "None",
                    60 to "1 min",
                    180 to "3 min",
                    300 to "5 min",
                    900 to "15 min"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    cooldownOptions.forEach { (sec, label) ->
                        val selected = cooldownSeconds == sec
                        AssistChip(
                            onClick = { cooldownSeconds = sec },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            var valid = true
                            if (name.isBlank()) {
                                nameError = true
                                valid = false
                            }
                            if (matchType != MatchType.FALLBACK_DEFAULT && pattern.isBlank()) {
                                patternError = true
                                valid = false
                            }
                            if (replyText.isBlank()) {
                                replyError = true
                                valid = false
                            }

                            if (valid) {
                                onSave(
                                    name.trim(),
                                    if (matchType == MatchType.FALLBACK_DEFAULT) "*" else pattern.trim(),
                                    matchType,
                                    replyText.trim(),
                                    isGroupAllowed,
                                    cooldownSeconds,
                                    if (matchType == MatchType.FALLBACK_DEFAULT) 0 else 10
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_rule_button")
                    ) {
                        Text("Save Rule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
