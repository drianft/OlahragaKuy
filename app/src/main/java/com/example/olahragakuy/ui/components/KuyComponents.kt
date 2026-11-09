package com.example.olahragakuy.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.olahragakuy.ui.theme.*
@Composable
fun KuyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = KuyButton, contentColor = KuyText),
        contentPadding = PaddingValues(10.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
@Composable
fun KuyTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    BasicTextField(value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(60.dp).semantics {
            contentDescription = label
            errorMessage?.let { error(it) }
        }, singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = KuyText),
        cursorBrush = SolidColor(KuyOrange), visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Surface(shape = RoundedCornerShape(12.dp), color = KuyNavy,
                border = BorderStroke(1.dp, if (errorMessage == null) KuyButton else KuyError)) {
                Box(Modifier.fillMaxSize().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(label, style = MaterialTheme.typography.bodySmall, color = KuyText)
                    innerTextField()
                }
            }
        })
}
