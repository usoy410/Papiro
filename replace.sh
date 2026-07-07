#!/bin/bash
sed -i '/val inference = synchronized(this) {/,/}/d' app/src/main/java/com/usoy/papiro/data/GeminiService.kt
