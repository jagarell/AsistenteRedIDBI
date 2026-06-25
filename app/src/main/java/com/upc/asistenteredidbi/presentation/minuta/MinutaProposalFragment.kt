package com.upc.asistenteredidbi.presentation.minuta

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentMinutaProposalBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MinutaProposalFragment : Fragment() {

    private var _binding: FragmentMinutaProposalBinding? = null
    private val binding get() = _binding!!

    private lateinit var equipmentAdapter: ProposalEquipmentAdapter
    private lateinit var recommendationAdapter: ProposalRecommendationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinutaProposalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()
        setupClicks()
        loadMockData()
    }

    private fun setupRecyclerViews() {
        equipmentAdapter = ProposalEquipmentAdapter()
        recommendationAdapter = ProposalRecommendationAdapter()

        binding.rvEquipment.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = equipmentAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEdit.setOnClickListener {
            findNavController().navigate(
                com.upc.asistenteredidbi.R.id.action_minutaProposalFragment_to_editProposalFragment,
                Bundle().apply {
                    putString("evaluationId", "demo-evaluation-001")
                }
            )
        }

        binding.btnSaveDraft.setOnClickListener {
            findNavController().navigate(
                R.id.action_minutaProposalFragment_to_sendDraftFragment,
                Bundle().apply {
                    putString("evaluationId", "demo-evaluation-001")
                }
            )
        }

        binding.btnGeneratePdf.setOnClickListener {
            Toast.makeText(requireContext(), "Generando PDF...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMockData() {
        equipmentAdapter.submitList(
            listOf(
                ProposalEquipmentItem(
                    R.drawable.ic_router,
                    "Cisco ISR 1100 Series Router",
                    "Router principal con firewall integrado",
                    "$450",
                    "Cant: 1"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_network,
                    "Cisco Catalyst 2960-X Switch",
                    "Switch administrable 24 puertos PoE",
                    "$760",
                    "Cant: 2"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_wifi,
                    "Ubiquiti UniFi AP AC PRO",
                    "Punto de acceso WiFi 6, cobertura 200m²",
                    "$1,000",
                    "Cant: 4"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_security,
                    "Fortinet FortiGate 60F",
                    "UTM/Firewall empresarial",
                    "$620",
                    "Cant: 1"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_energy,
                    "UPS APC Smart-UPS 1500VA",
                    "Respaldo de energía 15 min",
                    "$320",
                    "Cant: 1"
                )
            )
        )

        recommendationAdapter.submitList(
            listOf(
                ProposalRecommendationItem(1, "Implementar segmentación VLAN: POS (VLAN 10), WiFi clientes (VLAN 20), Gestión (VLAN 99)"),
                ProposalRecommendationItem(2, "Configurar QoS para priorizar tráfico crítico de punto de venta (máx latencia 5ms)"),
                ProposalRecommendationItem(3, "Instalar 2 APs adicionales en zona de cocina para cobertura completa sin puntos ciegos"),
                ProposalRecommendationItem(4, "Implementar redundancia de enlace con proveedor secundario (failover automático)"),
                ProposalRecommendationItem(5, "Configurar portal cautivo para WiFi de clientes con control de ancho de banda"),
                ProposalRecommendationItem(6, "Monitoreo proactivo con alertas 24/7 vía SNMP y syslog centralizado")
            )
        )
    }

    private fun setTopologyFromBase64(base64: String) {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imgTopology.setImageBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}