package com.example.rescateanimal

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

class RoleSelectorDialog(
    context: Context,
    private val roleManager: RoleManager,
    private val onRoleChanged: (String) -> Unit
) : Dialog(context) {

    private val TAG = "RoleSelectorDialog"
    private lateinit var tvCurrentRole: TextView
    private lateinit var rgRoleOptions: RadioGroup
    private lateinit var rbUser: RadioButton
    private lateinit var rbPartner: RadioButton
    private lateinit var rbAdmin: RadioButton
    private lateinit var tvRoleDescription: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_role_selector)
            Log.d(TAG, "Dialog layout set successfully")
            Log.d(TAG, "Dialog layout set successfully")

            initViews()
            setupCurrentRole()
            setupAvailableRoles()
            setupListeners()

            Log.d(TAG, "Dialog initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(context, "Error al crear el diálogo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        tvCurrentRole = findViewById(R.id.tvCurrentRole)
        rgRoleOptions = findViewById(R.id.rgRoleOptions)
        rbUser = findViewById(R.id.rbUser)
        rbPartner = findViewById(R.id.rbPartner)
        rbAdmin = findViewById(R.id.rbAdmin)
        tvRoleDescription = findViewById(R.id.tvRoleDescription)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)
    }

    private fun setupCurrentRole() {
        val currentRole = roleManager.getCurrentRole()
        tvCurrentRole.text = "Rol actual: ${roleManager.getRoleDisplayName(currentRole)}"

        // Marcar el rol actual
        when (currentRole) {
            RoleManager.ROLE_USER -> rbUser.isChecked = true
            RoleManager.ROLE_PARTNER -> rbPartner.isChecked = true
            RoleManager.ROLE_ADMIN -> rbAdmin.isChecked = true
        }

        // Mostrar descripción del rol actual
        tvRoleDescription.text = roleManager.getRoleDescription(currentRole)
    }

    private fun setupAvailableRoles() {
        val availableRoles = roleManager.getAvailableRoles()

        // Mostrar solo los roles disponibles
        rbUser.visibility = if (availableRoles.contains(RoleManager.ROLE_USER)) View.VISIBLE else View.GONE
        rbPartner.visibility = if (availableRoles.contains(RoleManager.ROLE_PARTNER)) View.VISIBLE else View.GONE
        rbAdmin.visibility = if (availableRoles.contains(RoleManager.ROLE_ADMIN)) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        // Cambiar descripción al seleccionar un rol
        rgRoleOptions.setOnCheckedChangeListener { _, checkedId ->
            val selectedRole = when (checkedId) {
                R.id.rbUser -> RoleManager.ROLE_USER
                R.id.rbPartner -> RoleManager.ROLE_PARTNER
                R.id.rbAdmin -> RoleManager.ROLE_ADMIN
                else -> RoleManager.ROLE_USER
            }
            tvRoleDescription.text = roleManager.getRoleDescription(selectedRole)
        }

        // Botón Cancelar
        btnCancel.setOnClickListener {
            dismiss()
        }

        // Botón Confirmar
        btnConfirm.setOnClickListener {
            val selectedRole = when (rgRoleOptions.checkedRadioButtonId) {
                R.id.rbUser -> RoleManager.ROLE_USER
                R.id.rbPartner -> RoleManager.ROLE_PARTNER
                R.id.rbAdmin -> RoleManager.ROLE_ADMIN
                else -> RoleManager.ROLE_USER
            }

            // Verificar si es diferente al rol actual
            if (selectedRole == roleManager.getCurrentRole()) {
                Toast.makeText(context, "Ya estás en este rol", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            Log.d(TAG, "Cambiando de ${roleManager.getCurrentRole()} a $selectedRole")

            // Cambiar el rol
            roleManager.switchRole(
                newRole = selectedRole,
                onSuccess = {
                    Log.d(TAG, "Rol cambiado exitosamente a: $selectedRole")
                    // Ejecutar el callback
                    onRoleChanged(selectedRole)
                    dismiss()
                },
                onError = { error ->
                    Log.e(TAG, "Error al cambiar rol: $error")
                    tvRoleDescription.text = "❌ Error: $error"
                }
            )
        }
    }
}