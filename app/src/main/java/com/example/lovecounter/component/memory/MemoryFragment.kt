package com.example.lovecounter.component.memory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovecounter.R
import com.example.lovecounter.base.adapter.BaseAdapter
import com.example.lovecounter.data.database.table.TableMemoryEntity
import com.example.lovecounter.data.repositories.ImageStorageRepository
import com.example.lovecounter.data.repositories.MemoryRepository
import com.example.lovecounter.databinding.FragmentMemoryBinding
import com.example.lovecounter.databinding.ItemMemoryCardBinding
import com.example.lovecounter.navigation.navigationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.lovecounter.utils.ext.loadLocalPreview
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint
class MemoryFragment : Fragment() {

    @Inject
    lateinit var imageStorageRepository: ImageStorageRepository

    @Inject
    lateinit var memoryRepository: MemoryRepository

    private var _binding: FragmentMemoryBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        MemoryAdapter(imageStorageRepository, viewLifecycleOwner.lifecycleScope)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Root-tab banner is owned by MainActivity so it persists between tabs.
        binding.bannerContainer.visibility = View.GONE

        binding.rvMemories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMemories.adapter = adapter

        val navigateToAdd = {
            navigationManager.navigate(AddMemoryFragment())
        }

        binding.fabAddMemoryEmpty.setOnClickListener { navigateToAdd() }
        binding.fabAddMemoryInline.setOnClickListener { navigateToAdd() }
        binding.fabAddMemoryPinned.setOnClickListener { navigateToAdd() }

        // Bottom nav actions
        binding.layoutEmptyState.setOnClickListener { } // no-op

        loadMemories()
    }

    private fun loadMemories() {
        viewLifecycleOwner.lifecycleScope.launch {
            memoryRepository.getAllMemoriesFlow().collectLatest { list ->
                if (list.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvMemories.visibility = View.GONE
                    binding.fabAddMemoryInline.visibility = View.GONE
                    binding.fabAddMemoryPinned.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvMemories.visibility = View.VISIBLE
                    adapter.submitList(list)

                    val layoutParams = binding.rvMemories.layoutParams as ConstraintLayout.LayoutParams
                    if (list.size >= 4) {
                        layoutParams.height = 0
                        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        layoutParams.bottomToTop = R.id.bannerContainer
                        layoutParams.verticalBias = 0.0f
                        binding.fabAddMemoryInline.visibility = View.GONE
                        binding.fabAddMemoryPinned.visibility = View.VISIBLE
                    } else {
                        layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        layoutParams.bottomToTop = R.id.bannerContainer
                        layoutParams.verticalBias = 0.0f
                        binding.fabAddMemoryInline.visibility = View.VISIBLE
                        binding.fabAddMemoryPinned.visibility = View.GONE
                    }
                    binding.rvMemories.layoutParams = layoutParams
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class MemoryAdapter(
        private val imageStorageRepository: ImageStorageRepository,
        private val scope: CoroutineScope
    ) : BaseAdapter<TableMemoryEntity, ItemMemoryCardBinding>() {
        override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemMemoryCardBinding {
            return ItemMemoryCardBinding.inflate(inflater, parent, false)
        }

        override fun bind(binding: ItemMemoryCardBinding, item: TableMemoryEntity, position: Int) {
            binding.tvMemoryTitle.text = item.title
            binding.tvMemoryDate.text = item.date
            binding.imgMemoryPhoto.loadLocalPreview(
                imageStorageRepository = imageStorageRepository,
                relativePath = item.imagePath,
                scope = scope,
                placeholderRes = R.drawable.bg_top_home
            )
        }
    }
}
