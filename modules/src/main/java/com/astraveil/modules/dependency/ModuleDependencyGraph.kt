package com.astraveil.modules.dependency

import com.astraveil.core.logger.AstraLogger

/**
 * Module Dependency Graph with Capability Inheritance.
 *
 * Modules can declare dependencies on other modules. This graph tracks
 * the dependency DAG, detects cycles, computes topological sort order
 * (for module start sequencing), and supports capability inheritance
 * from dependencies.
 *
 * Inspired by Linux CAP_INHERITABLE, applied at the module level.
 */
class ModuleDependencyGraph {

    data class Dependency(
        val moduleId: String,
        val minVersion: String = "0.0.0",
        val optional: Boolean = false,
    )

    data class Node(
        val moduleId: String,
        val version: String,
        val capabilities: Set<String>,
        val dependencies: List<Dependency>,
    )

    private val nodes = mutableMapOf<String, Node>()
    private val adjacency = mutableMapOf<String, MutableSet<String>>()
    private val reverseAdj = mutableMapOf<String, MutableSet<String>>()

    fun addModule(node: Node, installedModules: Set<String>) {
        for (dep in node.dependencies) {
            if (!dep.optional && dep.moduleId !in installedModules && dep.moduleId != node.moduleId) {
                throw MissingDependencyException(
                    "Module '${node.moduleId}' requires '${dep.moduleId}' " +
                    "(min ${dep.minVersion}) which is not installed"
                )
            }
        }

        nodes[node.moduleId] = node
        adjacency.getOrPut(node.moduleId) { mutableSetOf() }
        reverseAdj.getOrPut(node.moduleId) { mutableSetOf() }

        for (dep in node.dependencies) {
            adjacency.getOrPut(dep.moduleId) { mutableSetOf() }.add(node.moduleId)
            reverseAdj.getOrPut(node.moduleId) { mutableSetOf() }.add(dep.moduleId)
        }

        if (hasCycle()) {
            removeModule(node.moduleId)
            throw CircularDependencyException(
                "Adding '${node.moduleId}' would create a circular dependency"
            )
        }

        AstraLogger.i(TAG, "Module '${node.moduleId}' added to dependency graph " +
            "(deps: ${node.dependencies.map { it.moduleId }})")
    }

    fun removeModule(moduleId: String) {
        nodes.remove(moduleId)
        adjacency.remove(moduleId)?.forEach { child ->
            reverseAdj[child]?.remove(moduleId)
        }
        reverseAdj.remove(moduleId)?.forEach { parent ->
            adjacency[parent]?.remove(moduleId)
        }
    }

    fun effectiveCapabilities(
        moduleId: String,
        inheritableCaps: Map<String, Set<String>> = emptyMap(),
    ): Set<String> {
        val node = nodes[moduleId] ?: return emptySet()
        val effective = node.capabilities.toMutableSet()

        for (dep in node.dependencies) {
            val depInheritable = inheritableCaps[dep.moduleId] ?: continue
            effective.addAll(depInheritable)
        }

        return effective
    }

    fun topologicalSort(): List<String>? {
        val inDegree = mutableMapOf<String, Int>()
        nodes.keys.forEach { inDegree[it] = 0 }
        adjacency.forEach { (_, children) ->
            children.forEach { child ->
                inDegree[child] = (inDegree[child] ?: 0) + 1
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }.forEach { queue.add(it.key) }

        val sorted = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            sorted.add(current)
            adjacency[current]?.forEach { child ->
                val newDegree = (inDegree[child] ?: 1) - 1
                inDegree[child] = newDegree
                if (newDegree == 0) queue.add(child)
            }
        }

        return if (sorted.size == nodes.size) sorted else null
    }

    fun dependentsOf(moduleId: String): Set<String> {
        val result = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        adjacency[moduleId]?.forEach { stack.add(it) }
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (result.add(current)) {
                adjacency[current]?.forEach { stack.add(it) }
            }
        }
        return result
    }

    private fun hasCycle(): Boolean {
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun dfs(node: String): Boolean {
            if (node in inStack) return true
            if (node in visited) return false
            visited.add(node)
            inStack.add(node)
            for (child in adjacency[node] ?: emptySet()) {
                if (dfs(child)) return true
            }
            inStack.remove(node)
            return false
        }

        return nodes.keys.any { dfs(it) }
    }

    companion object {
        private const val TAG = "ModuleDependencyGraph"
    }
}

class CircularDependencyException(message: String) : Exception(message)
class MissingDependencyException(message: String) : Exception(message)
