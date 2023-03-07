package br.android.cericatto.loadingstatus

sealed class ButtonState {
    object UnClicked : ButtonState()
    object Loading : ButtonState()
    object Completed : ButtonState()
}