/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Gabriel Bender
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.explainable.corelang;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Class representing a conjunction of relational views under Cohen semantics.
 */
public final class Conjunction {
	private final ImmutableList<Atom> atoms;

	private Conjunction(List<Atom> atoms) {
		this.atoms = ImmutableList.copyOf(atoms);
	}

	public static Conjunction create(List<Atom> atoms) {
		return new Conjunction(atoms);
	}

	public ImmutableList<Atom> atoms() {
		return atoms;
	}

	@Nullable
	private Homomorphism findHomomorphism(
			Conjunction other,
			Homomorphism partialHom,
			int sourceIndex) {
		if (sourceIndex == atoms.size()) {
			return partialHom;
		}

		Atom source = atoms.get(sourceIndex);

		for (int targetIndex = 0; targetIndex < other.atoms.size(); targetIndex++) {
			Atom target = other.atoms.get(targetIndex);
			Homomorphism candidate = partialHom.extend(source, target);

			if (candidate != null) {
				Homomorphism extension = findHomomorphism(other, candidate, sourceIndex + 1);
				if (extension != null) {
					return extension;
				}
			}
		}

		return null;
	}

	/**
	 * Apply {@code termMap} to the terms of the current object.
	 *
	 * @param termMap the map which should be applied to the conjunction's terms
	 * @return a new view obtained by applying the current map to the specified view
	 */
	public Conjunction apply(TermMap termMap) {
		List<Atom> newAtoms = Lists.newArrayListWithCapacity(atoms.size());

		for (Atom atom : atoms) {
			newAtoms.add(atom.apply(termMap));
		}

		return new Conjunction(newAtoms);
	}

	/**
	 * Determine whether there is a homomorphism from the current onto {@code other}. There is one
	 * technical difference between the homomorphisms used in this method and the formal definition
	 * provided by Cohen: we do not require every distinguished variable in {@code other} to appear
	 * in the homomorphism's image.
	 *
	 * @param other the conjunction that we want to find a homomorphism onto.
	 * @return a homomorphism from the current object to other, or {@code null} if none exists
	 */
	@Nullable
	Homomorphism findHomomorphism(Conjunction other) {
		return findHomomorphism(other, new Homomorphism(), 0);
	}

	public boolean isHomomorphicTo(Conjunction other) {
		return findHomomorphism(other) != null && other.findHomomorphism(this) != null;
	}

	@Override
	public int hashCode() {
		return atoms.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Conjunction)) {
			return false;
		}
		Conjunction otherConj = (Conjunction) other;
		return atoms.equals(otherConj.atoms);
	}

	@Override
	public String toString() {
		return new ViewToString("Q", ViewToStringMode.ADVANCED)
				.appendAllBodyAtoms(atoms)
				.toString();
	}
}
