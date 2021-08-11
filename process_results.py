#!/usr/bin/env python3

import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

def plot_windows(ax, labels, **kwds):
    in_window = False
    win_start = 0
    win_end = 0
    for i, is_anomaly in enumerate(labels):
        if in_window:
            if not is_anomaly:
                win_end = i
                in_window = False
                ax.axvspan(win_start, win_end, **kwds)
        else:
            if is_anomaly:
                win_start = i
                in_window = True


def plot_results(df, filename='explore.png', title='results', xmin=None, xmax=None):
    if xmin is None:
        xmin = 0
    if xmax is None:
        xmax = len(df)

    anomalous_scores = df['score'].where(df['predicted'] == 1)

    fig, ax = plt.subplots(3, 1, figsize=(12, 8), dpi=100)

    ax[0].plot(df['data0'])
    ax[0].set_title(title)
    ax[0].set_ylabel('data0')
    ax[0].set_xticklabels([])
    ax[0].grid(axis='x')

    ax[1].plot(df['score'], color='C1', label='score')
    #ax[1].plot(df['grade'], color='C3', label='grade')
    #ax[1].plot(anomalous_scores, marker='o', markersize=5, color='C3', label='anomalies')
    ax[1].set_ylabel('score', color='C1')
    ax[1].grid(True)

    ax[2].plot(df['grade'], color='C3', label='grade')
    ax[2].set_ylabel('grade', color='C3')
    ax[2].set_ylim(0, 1.1)
    ax[2].set_yticks([0, 0.5, 1.0])
    ax[2].grid(True)

    for axi in ax:
        #plot_windows(axi, df['predicted'], color='C3', alpha=0.4)
        #plot_windows(axi, df['label'], color='C3', alpha=0.2)
        #axi.set_xlim(xmin, xmax)
        pass

    fig.tight_layout()
    fig.subplots_adjust(hspace=0)
    fig.savefig(filename)


def main(output_dir, result_filename, plot_kwds):
    output_dir = os.path.abspath(output_dir)
    filename = os.path.join(output_dir, f"results_{result_filename}.csv")
    print(f'[explore.py] exploring {filename}')

    df = pd.read_csv(filename)
    plot_results(df, **plot_kwds)


def build_parser():
    import argparse
    parser = argparse.ArgumentParser(description='explore experiment results')
    parser.add_argument('output_dir', type=str, help='''
        the directory continain output results.''')
    parser.add_argument('result_filename', type=str, help='''
        the base filename of a results file. for example, if the actual filename
        is "results_foo.csv" then this should be set to "foo"''')
    parser.add_argument('plot_filename', type=str, help='''
        name of the output plot. include image file extensions''')
    parser.add_argument('--xmin', type=int, default=None, help='(default: 0) xmin of plot')
    parser.add_argument('--xmax', type=int, default=None, help='(default: #data) xmax of plot')
    return parser


if __name__ == '__main__':
    parser = build_parser()
    args = parser.parse_args()

    plot_kwds = dict(
        filename=args.plot_filename,
        title=args.result_filename,
        xmin=args.xmin,
        xmax=args.xmax,
    )
    main(args.output_dir, args.result_filename, plot_kwds)