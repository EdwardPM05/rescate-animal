package com.example.rescateanimal

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial

data class AdvancedFilters(
    var petType: String? = null,           // "perro", "gato", "otro"
    var size: String? = null,              // "pequeño", "mediano", "grande"
    var ageRange: String? = null,          // "cachorro", "joven", "adulto", "senior"
    var isVaccinated: Boolean? = null,
    var isSterilized: Boolean? = null
)

class AdvancedFiltersDialog(
    context: Context,
    private val currentFilters: AdvancedFilters,
    private val onApplyFilters: (AdvancedFilters) -> Unit
) : Dialog(context) {

    // Pet Type
    private lateinit var chipTypePerro: LinearLayout
    private lateinit var chipTypeGato: LinearLayout
    private lateinit var chipTypeOtro: LinearLayout

    // Size
    private lateinit var chipSizePequeno: Chip
    private lateinit var chipSizeMediano: Chip
    private lateinit var chipSizeGrande: Chip

    // Age
    private lateinit var chipGroupAge: ChipGroup
    private lateinit var chipAgeCachorro: Chip
    private lateinit var chipAgeJoven: Chip
    private lateinit var chipAgeAdulto: Chip
    private lateinit var chipAgeSenior: Chip

    // Health
    private lateinit var switchVaccinated: SwitchMaterial
    private lateinit var switchSterilized: SwitchMaterial

    // Buttons
    private lateinit var btnClearFilters: Button
    private lateinit var btnApplyFilters: Button

    private var selectedPetType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_advanced_filters)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        initializeViews()
        loadCurrentFilters()
        setupListeners()
    }

    private fun initializeViews() {
        // Pet Type LinearLayouts
        chipTypePerro = findViewById(R.id.chipTypePerro)
        chipTypeGato = findViewById(R.id.chipTypeGato)
        chipTypeOtro = findViewById(R.id.chipTypeOtro)

        // Size chips
        chipSizePequeno = findViewById(R.id.chipSizePequeno)
        chipSizeMediano = findViewById(R.id.chipSizeMediano)
        chipSizeGrande = findViewById(R.id.chipSizeGrande)

        // Age chips
        chipGroupAge = findViewById(R.id.chipGroupAge)
        chipAgeCachorro = findViewById(R.id.chipAgeCachorro)
        chipAgeJoven = findViewById(R.id.chipAgeJoven)
        chipAgeAdulto = findViewById(R.id.chipAgeAdulto)
        chipAgeSenior = findViewById(R.id.chipAgeSenior)

        // Health switches
        switchVaccinated = findViewById(R.id.switchVaccinated)
        switchSterilized = findViewById(R.id.switchSterilized)

        // Buttons
        btnClearFilters = findViewById(R.id.btnClearFilters)
        btnApplyFilters = findViewById(R.id.btnApplyFilters)
    }

    private fun loadCurrentFilters() {
        // Pet Type
        selectedPetType = currentFilters.petType
        updatePetTypeSelection()

        // Size
        when (currentFilters.size) {
            "pequeño" -> chipSizePequeno.isChecked = true
            "mediano" -> chipSizeMediano.isChecked = true
            "grande" -> chipSizeGrande.isChecked = true
        }

        // Age
        when (currentFilters.ageRange) {
            "cachorro" -> chipAgeCachorro.isChecked = true
            "joven" -> chipAgeJoven.isChecked = true
            "adulto" -> chipAgeAdulto.isChecked = true
            "senior" -> chipAgeSenior.isChecked = true
        }

        // Health
        switchVaccinated.isChecked = currentFilters.isVaccinated ?: false
        switchSterilized.isChecked = currentFilters.isSterilized ?: false
    }

    private fun setupListeners() {
        // Pet Type click listeners
        chipTypePerro.setOnClickListener {
            selectedPetType = if (selectedPetType == "perro") null else "perro"
            updatePetTypeSelection()
        }

        chipTypeGato.setOnClickListener {
            selectedPetType = if (selectedPetType == "gato") null else "gato"
            updatePetTypeSelection()
        }

        chipTypeOtro.setOnClickListener {
            selectedPetType = if (selectedPetType == "otro") null else "otro"
            updatePetTypeSelection()
        }

        // Size chips - Solo uno puede estar seleccionado
        chipSizePequeno.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chipSizeMediano.isChecked = false
                chipSizeGrande.isChecked = false
            }
        }

        chipSizeMediano.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chipSizePequeno.isChecked = false
                chipSizeGrande.isChecked = false
            }
        }

        chipSizeGrande.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chipSizePequeno.isChecked = false
                chipSizeMediano.isChecked = false
            }
        }

        // Age chips - Ya configurado con ChipGroup singleSelection en XML
        // No necesita listener adicional

        // Clear filters button
        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        // Apply filters button
        btnApplyFilters.setOnClickListener {
            applyFilters()
        }
    }

    private fun updatePetTypeSelection() {
        // Reset all to unselected state
        chipTypePerro.setBackgroundResource(R.drawable.tab_unselected)
        chipTypeGato.setBackgroundResource(R.drawable.tab_unselected)
        chipTypeOtro.setBackgroundResource(R.drawable.tab_unselected)

        // Update text colors
        updatePetTypeTextColor(chipTypePerro, false)
        updatePetTypeTextColor(chipTypeGato, false)
        updatePetTypeTextColor(chipTypeOtro, false)

        // Set selected state
        when (selectedPetType) {
            "perro" -> {
                chipTypePerro.setBackgroundResource(R.drawable.tab_selected)
                updatePetTypeTextColor(chipTypePerro, true)
            }
            "gato" -> {
                chipTypeGato.setBackgroundResource(R.drawable.tab_selected)
                updatePetTypeTextColor(chipTypeGato, true)
            }
            "otro" -> {
                chipTypeOtro.setBackgroundResource(R.drawable.tab_selected)
                updatePetTypeTextColor(chipTypeOtro, true)
            }
        }
    }

    private fun updatePetTypeTextColor(layout: LinearLayout, isSelected: Boolean) {
        val textView = layout.getChildAt(1) as? TextView
        val color = if (isSelected) {
            // Blanco cuando está seleccionado (fondo naranja)
            android.graphics.Color.WHITE
        } else {
            // Gris cuando no está seleccionado
            android.graphics.Color.parseColor("#757575")
        }
        textView?.setTextColor(color)
    }

    private fun clearAllFilters() {
        // Pet Type
        selectedPetType = null
        updatePetTypeSelection()

        // Size
        chipSizePequeno.isChecked = false
        chipSizeMediano.isChecked = false
        chipSizeGrande.isChecked = false

        // Age
        chipGroupAge.clearCheck()

        // Health
        switchVaccinated.isChecked = false
        switchSterilized.isChecked = false
    }

    private fun applyFilters() {
        val filters = AdvancedFilters()

        // Pet Type
        filters.petType = selectedPetType

        // Size
        filters.size = when {
            chipSizePequeno.isChecked -> "pequeño"
            chipSizeMediano.isChecked -> "mediano"
            chipSizeGrande.isChecked -> "grande"
            else -> null
        }

        // Age range
        filters.ageRange = when {
            chipAgeCachorro.isChecked -> "cachorro"
            chipAgeJoven.isChecked -> "joven"
            chipAgeAdulto.isChecked -> "adulto"
            chipAgeSenior.isChecked -> "senior"
            else -> null
        }

        // Health
        filters.isVaccinated = if (switchVaccinated.isChecked) true else null
        filters.isSterilized = if (switchSterilized.isChecked) true else null

        onApplyFilters(filters)
        dismiss()
    }
}