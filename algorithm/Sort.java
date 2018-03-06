package book;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Sort {
	static int count = 0;

	public static void main(String[] args) {
		int[] a = { 1, 3, 2, 7, 1, 4, 5, 9, 8, 7, 7, 4, 6 };
		// int[] a={1,1,1,1,1};
		// insertionSort(a);
		// bubbleSort(a);
		// quickSort(a, 0, a.length-1);
		// mergeSort(a, 0, a.length-1);
		int[] temp = new int[a.length];
		// quickSort2(a, 0, a.length - 1);
		printArray(a);
//		// mergeSort2(a, temp, 0, a.length-1);
//		headSort(a);
//		System.out.println();
//		RadixSortLSD(a);
		RadixSortMSD(a);
		printArray(a);
		LinkedList<Integer> linkedList = new LinkedList<>();
		// testMaxHeap();
//		System.out.println(10^2);
//		System.out.println(155732);
//		for (int i = 1; i < 5; i++) {
//			System.out.println(i+" "+getIndex(155732, i));
//		}
	}

//	public static void testMaxHeap() {
//		int[] a = new int[] { 2, 4, 1, 3, 5 };
//		maxHeap(a, 0,);
//		print(a, "0");
//		a = new int[] { 2, 4, 1, 3, 5 };
//		maxHeap(a, 1);
//		print(a, "1");
//		a = new int[] { 2, 4, 1, 3, 5 };
//		maxHeap(a, 3);
//		print(a, "2");
//	}
	
	
	public static void RadixSortLSD(int[] array) {
		int d = getDigLength();
		List<LinkedList<Integer>> buketList = new ArrayList<LinkedList<Integer>>(d);
		for (int i = 0; i < d; i++) {
			buketList.add(new LinkedList<Integer>());
		}
		for (int i = d; i >0; i--) {
			for (int j = 0; j < array.length; j++) {
				int index = getIndex(array[j],i);
				buketList.get(index).add(array[j]);
			}
			int count = 0;
			for (LinkedList<Integer> linkedList : buketList) {
				for (Integer integer : linkedList) {
					array[count] = integer;
					count++;
				}
				linkedList.clear();
			}
		}
	}

	public static void RadixSortMSD(int[] array) {
		int d = getDigLength();
		List<LinkedList<Integer>> buketList = new ArrayList<LinkedList<Integer>>(d);
		for (int i = 0; i < d; i++) {
			buketList.add(new LinkedList<Integer>());
		}
		for (int i = 1; i <= d; i++) {
			for (int j = 0; j < array.length; j++) {
				int index = getIndex(array[j],i);
				buketList.get(index).add(array[j]);
			}
			int count = 0;
			for (LinkedList<Integer> linkedList : buketList) {
				for (Integer integer : linkedList) {
					array[count] = integer;
					count++;
				}
				linkedList.clear();
			}
		}
	}
	
	public static int getDigLength() {
		return 10;
	}

	public static int getIndex(int ArrayValue,int digIndex) {
		int value =  (ArrayValue%((int)(Math.pow(10, digIndex))));
		if (digIndex>1) {
			value = value/((int)(Math.pow(10, digIndex-1)));
		}
		return value;
	}
	
	public static void print(int[] a, String name) {
		System.out.print(name + ": ");
		for (int i = 0; i < a.length; i++) {
			System.out.print(a[i]);
		}
		System.out.println();
	}

	public static void headSort(int[] array) {
		bulidMaxHeap(array);
		for (int i = array.length - 1; i >0; i--) {
			swap(array, 0, i);
			maxHeap(array, 0,i);	//这里构建最大堆是有范围限制的,因为i+1->n-1都是已经排好序的了,就不用再去动它了
		}
	}

	/**
	 * 对全局构建最大堆
	 * @param array
	 */
	public static void bulidMaxHeap(int[] array) {
		for (int i = (array.length - 2) / 2; i >= 0; i--) {
			maxHeap(array, i,array.length-1);
		}
		print(array);
	}

	private static void print(int[] data) {
		int pre = -2;
		for (int i = 0; i < data.length; i++) {
			// 换行
			if (pre < (int) getLog(i + 1)) {
				pre = (int) getLog(i + 1);
				System.out.println();
			}
			System.out.print(data[i] + " |");
		}
	}

	/**
	 * 以2为底的对数
	 * 
	 * @param param
	 * @return
	 */
	private static double getLog(double param) {
		return Math.log(param) / Math.log(2);
	}

	public static void maxHeap(int[] array, int node,int maxIndex) {
		int left = left(node), right = right(node), changedIndex = node;

		if (left < maxIndex && array[left] > array[changedIndex]) {
			changedIndex = left;
		}
		if (right < maxIndex && array[right] > array[changedIndex]) {
			changedIndex = right;
		}

		if (changedIndex != node) {
			swap(array, changedIndex, node);
			maxHeap(array, changedIndex,maxIndex);
		}
	}

	public static int left(int i) {
		return 2 * i + 1;
	}

	public static int right(int i) {
		return 2 * i + 2;
	}

	public static void mergeSort2(int[] array, int[] temp, int left, int right) {
		if (left < right) {
			int middle = (left + right) / 2;
			mergeSort2(array, temp, left, middle);
			mergeSort2(array, temp, middle + 1, right);
			merge2(array, temp, left, right, middle);
		}
	}

	public static void merge2(int[] array, int[] temp, int left, int right,
			int middle) {
		int l = left, r = middle + 1, tempindex = left;
		while (l <= middle && r <= right) {
			if (array[l] > array[r]) {
				temp[tempindex++] = array[r++];
			} else {
				temp[tempindex++] = array[l++];
			}
		}
		while (l <= middle) {
			temp[tempindex++] = array[l++];
		}
		while (r <= right) {
			temp[tempindex++] = array[r++];
		}
		for (int i = left; i <= right; i++) {
			array[i] = temp[i];
			System.out.print(temp[i] + " ");
		}
		System.out.println();
	}

	public static void quickSort2(int[] array, int left, int right) {
		if (left < right) {
			int index = partition(array, left, right);
			quickSort2(array, left, index - 1);
			quickSort2(array, index + 1, right);
		}
	}

	private static int partition(int[] array, int left, int right) {
		Random random = new Random();
		int flatNum = random.nextInt(right - left + 1) + left;
		swap(array, left, flatNum);
		int i = left + 1, j = right;
		while (i <= j) {
			if (array[left] < array[i]) {
				swap(array, i, j);
				j--;
			} else {
				i++;
			}
		}
		swap(array, left, j);
		return j;

	}

	/**
	 * 简单选择排序
	 * 
	 * @param array
	 */
	public static void Sort2(int[] array) {
		for (int i = 0; i < array.length; i++) {
			for (int j = i + 1; j < array.length; j++) {
				if (array[j] < array[i]) {
					swap(array, i, j);
				}
			}
		}
	}

	public static void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	public static void insertionSort(int[] array) {
		int temp;
		int index = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] < array[i - 1]) {
				for (int j = 0; j < i; j++) {
					if (array[i] < array[j]) {
						index = j;
						break;
					}
				}
				for (int j = i; j > index; j--) {
					temp = array[j];
					array[j] = array[j - 1];
					array[j - 1] = temp;
				}
				count++;
			}
		}
	}

	public static void bubbleSort(int[] array) {
		boolean isExchange = true;
		int temp;
		for (int i = 1; i < array.length - 1 && isExchange; i++) {
			isExchange = false;
			for (int j = 0; j < array.length - i; j++) {
				if (array[j] > array[j + 1]) {
					temp = array[j];
					array[j] = array[j + 1];
					array[j + 1] = temp;
					isExchange = true;
				}
			}
		}
	}

	public static void quickSort(int[] array, int left, int right) {
		if (left >= right) {
			return;
		}
		int p = partion(array, left, right);
		quickSort(array, left, p - 1);
		quickSort(array, p + 1, right);

	}

	public static int partion(int[] array, int left, int right) {
		Random random = new Random();
		int flatNum = random.nextInt(right - left + 1) + left;
		exchange(array, left, flatNum);
		int nextCompareIndex = left + 1, comparedBigger = right + 1;
		while (nextCompareIndex < comparedBigger) {
			if (array[nextCompareIndex] > array[left]) {
				exchange(array, nextCompareIndex, --comparedBigger);
			} else {
				nextCompareIndex++;
			}
		}
		exchange(array, --comparedBigger, left);
		return comparedBigger;

	}

	public static void mergeSort(int[] array, int left, int right) {
		if (left >= right) {
			return;
		}
		mergeSort(array, left, (left + right) / 2);
		mergeSort(array, (left + right) / 2 + 1, right);
		merge(array, left, (left + right) / 2, right);
	}

	public static void merge(int[] array, int left, int middle, int right) {
		int rightIndex = middle + 1, i = 0, recordLeft = left;
		int[] tempArray = new int[right - left + 1];
		for (; left <= middle && rightIndex <= right; i++) {
			if (array[left] <= array[rightIndex]) {
				tempArray[i] = array[left++];
			} else {
				tempArray[i] = array[rightIndex++];
			}
		}
		while (left <= middle) {
			tempArray[i++] = array[left++];
		}
		while (rightIndex <= right) {
			tempArray[i++] = array[rightIndex++];
		}
		for (int j = 0; j < tempArray.length; j++) {
			array[recordLeft++] = tempArray[j];
		}

	}

	public static void exchange(int[] array, int index1, int index2) {
		int temp = array[index1];
		array[index1] = array[index2];
		array[index2] = temp;
	}

	public static void printArray(int[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i] + " ");
		}
		System.out.println("count:" + count);
	}
}
