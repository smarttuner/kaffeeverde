/*
 *    Original source code is Copyright (c) 2021 Tlaster
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy
 *    of this software and associated documentation files (the "Software"), to deal
 *    in the Software without restriction, including without limitation the rights
 *    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *    copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all
 *    copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *    SOFTWARE.
 *
 *
 *
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://github.com/Tlaster/PreCompose/blob/master/precompose/src/commonMain/kotlin/moe/tlaster/precompose/ui/ComposeCompositionLocal.kt
 *
 * The content of this file is a port of the original work with some additions
 *
 *
 */
package net.smarttuner.kaffeeverde.lifecycle.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner

val LocalViewModelStoreOwner = compositionLocalOf<ViewModelStoreOwner?> { null }
