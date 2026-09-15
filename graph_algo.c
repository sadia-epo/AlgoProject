
                                                    /*Intelligent Route Optimization System Using Graph Algorithms*/



#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define MAXN 30
#define INF 999999
int n;
int m;
char names[MAXN][50];

int distMat[MAXN][MAXN];
int costMat[MAXN][MAXN];
int timeMat[MAXN][MAXN];

int weight[MAXN][MAXN];

int source, destination;
int criteria;
int algoChoice;


#define MAXALLROUTES 30
int allRoutePaths[MAXALLROUTES][MAXN];
int allRouteLens[MAXALLROUTES];
int allRouteWeights[MAXALLROUTES];   /* weight under the chosen criteria */
int allRouteCount;
int visitedForSearch[MAXN];
int currentSearchPath[MAXN];
int currentSearchLen;

FILE *outFile;


void readInput(char *filename)
{
    FILE *fp = fopen(filename, "r");
    if (fp == NULL) {
        printf("Error: cannot open input file %s\n", filename);
        exit(1);
    }

    fscanf(fp, "%d %d", &n, &m);

    for (int i = 0; i < n; i++)
        fscanf(fp, "%s", names[i]);

    /* start with "no route" everywhere except a node to itself */
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            distMat[i][j] = (i == j) ? 0 : INF;
            costMat[i][j] = (i == j) ? 0 : INF;
            timeMat[i][j] = (i == j) ? 0 : INF;
        }
    }

    for (int k = 0; k < m; k++) {
        int u, v, d, c, t;
        fscanf(fp, "%d %d %d %d %d", &u, &v, &d, &c, &t);
        /* routes are treated as two-way (undirected) */
        distMat[u][v] = d;  distMat[v][u] = d;
        costMat[u][v] = c;  costMat[v][u] = c;
        timeMat[u][v] = t;  timeMat[v][u] = t;
    }

    fscanf(fp, "%d %d %d %d", &source, &destination, &criteria, &algoChoice);

    fclose(fp);
}


void buildWeightMatrix(int c)
{
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            if (c == 1)      weight[i][j] = distMat[i][j];
            else if (c == 2) weight[i][j] = costMat[i][j];
            else             weight[i][j] = timeMat[i][j];
        }
    }
}

/* ---------------- Alternative Routes ---------------- */



void searchAllRoutes(int u)
{
    visitedForSearch[u] = 1;
    currentSearchPath[currentSearchLen++] = u;

    if (u == destination) {
        if (allRouteCount < MAXALLROUTES) {
            for (int i = 0; i < currentSearchLen; i++)
                allRoutePaths[allRouteCount][i] = currentSearchPath[i];
            allRouteLens[allRouteCount] = currentSearchLen;
            allRouteCount++;
        }
    } else if (allRouteCount < MAXALLROUTES) {
        for (int v = 0; v < n; v++) {
            if (!visitedForSearch[v] && weight[u][v] != INF) {
                searchAllRoutes(v);
            }
        }
    }

    /* backtrack: unmark this node so other routes can use it too */
    visitedForSearch[u] = 0;
    currentSearchLen--;
}



/* finds every route, ranks them by the chosen criteria (best first),
   and writes them all to the output file
                              */
void findAndPrintAllRoutes()
{
    allRouteCount = 0;
    currentSearchLen = 0;
    for (int i = 0; i < n; i++)
      visitedForSearch[i] = 0;

    searchAllRoutes(source);

    /* compute each route's total weight under the chosen criteria */
    for (int i = 0; i < allRouteCount; i++) {
        int total = 0;
        for (int j = 0; j < allRouteLens[i] - 1; j++) {
            int u = allRoutePaths[i][j], v = allRoutePaths[i][j + 1];
            total += weight[u][v];
        }
        allRouteWeights[i] = total;
    }


    /* simple selection sort - fine since allRouteCount is small */
    for (int i = 0; i < allRouteCount - 1; i++) {
        int best = i;
        for (int j = i + 1; j < allRouteCount; j++)
            if (allRouteWeights[j] < allRouteWeights[best])
                best = j;
        if (best != i) {
            int tmpW = allRouteWeights[i]; allRouteWeights[i] = allRouteWeights[best]; allRouteWeights[best] = tmpW;
            int tmpLen = allRouteLens[i]; allRouteLens[i] = allRouteLens[best]; allRouteLens[best] = tmpLen;
            int tmpPath[MAXN];
            memcpy(tmpPath, allRoutePaths[i], sizeof(tmpPath));
            memcpy(allRoutePaths[i], allRoutePaths[best], sizeof(tmpPath));
            memcpy(allRoutePaths[best], tmpPath, sizeof(tmpPath));
        }
    }

    fprintf(outFile, "ALL_ROUTES_START\n");
    fprintf(outFile, "ALL_ROUTES_COUNT=%d\n", allRouteCount);
    for (int i = 0; i < allRouteCount; i++) {
        int totalDist = 0, totalCost = 0, totalTime = 0;
        for (int j = 0; j < allRouteLens[i] - 1; j++) {
            int u = allRoutePaths[i][j], v = allRoutePaths[i][j + 1];
            totalDist += distMat[u][v];
            totalCost += costMat[u][v];
            totalTime += timeMat[u][v];
        }

        fprintf(outFile, "ROUTE_PATH=");
        for (int j = 0; j < allRouteLens[i]; j++) {
            fprintf(outFile, "%s", names[allRoutePaths[i][j]]);
            if (j != allRouteLens[i] - 1) fprintf(outFile, " -> ");
        }
        fprintf(outFile, "\n");
        fprintf(outFile, "ROUTE_DISTANCE=%d\n", totalDist);
        fprintf(outFile, "ROUTE_COST=%d\n", totalCost);
        fprintf(outFile, "ROUTE_TIME=%d\n", totalTime);
        fprintf(outFile, "ROUTE_BEST=%s\n", (i == 0) ? "YES" : "NO");
    }
    fprintf(outFile, "ALL_ROUTES_END\n");
}



/* writes one algorithm's result block to the output file */
void printResult(char *algoName, int path[], int pathLen, int totalWeight, double timeMs)
{
    int totalDist = 0, totalCost = 0, totalTime = 0;

    for (int i = 0; i < pathLen - 1; i++) {
        int u = path[i], v = path[i + 1];
        totalDist += distMat[u][v];
        totalCost += costMat[u][v];
        totalTime += timeMat[u][v];
    }

    fprintf(outFile, "ALGORITHM=%s\n", algoName);
    fprintf(outFile, "PATH=");
    for (int i = 0; i < pathLen; i++) {
        fprintf(outFile, "%s", names[path[i]]);
        if (i != pathLen - 1) fprintf(outFile, " -> ");
    }
    fprintf(outFile, "\n");

    if (totalWeight >= INF) {
        fprintf(outFile, "STATUS=NO_PATH\n");
    } else {
        fprintf(outFile, "STATUS=OK\n");
        fprintf(outFile, "TOTAL_DISTANCE=%d\n", totalDist);
        fprintf(outFile, "TOTAL_COST=%d\n", totalCost);
        fprintf(outFile, "TOTAL_TIME=%d\n", totalTime);
    }
    fprintf(outFile, "EXECUTION_TIME_MS=%.5f\n", timeMs);
    fprintf(outFile, "----\n");
}






/* ---------------- Dijkstra's Algorithm ----------------*/

void dijkstra()
{
    int dist[MAXN], visited[MAXN], parent[MAXN];
    clock_t start = clock();

    for (int i = 0; i < n; i++) {
        dist[i] = INF;
        visited[i] = 0;
        parent[i] = -1;
    }
    dist[source] = 0;

    for (int count = 0; count < n - 1; count++) {
        int u = -1, minDist = INF;
        for (int i = 0; i < n; i++) {
            if (!visited[i] && dist[i] < minDist) {
                minDist = dist[i];
                u = i;
            }
        }
        if (u == -1) break;   /* rest of the nodes are unreachable */
        visited[u] = 1;

        for (int v = 0; v < n; v++) {
            if (!visited[v] && weight[u][v] != INF &&
                dist[u] + weight[u][v] < dist[v]) {
                dist[v] = dist[u] + weight[u][v];
                parent[v] = u;
            }
        }
    }

    clock_t end = clock();
    double timeMs = ((double)(end - start)) * 1000.0 / CLOCKS_PER_SEC;

    int path[MAXN], pathLen = 0;
    if (dist[destination] < INF) {
        int cur = destination;
        while (cur != -1) {
            path[pathLen++] = cur;
            cur = parent[cur];
        }
        for (int i = 0; i < pathLen / 2; i++) {
            int temp = path[i];
            path[i] = path[pathLen - 1 - i];
            path[pathLen - 1 - i] = temp;
        }
    } else {
        path[0] = source;
        pathLen = 1;
    }

    printResult("Dijkstra", path, pathLen, dist[destination], timeMs);
}





/* ---------------- Bellman-Ford Algorithm ----------------*/

void bellmanFord()
{
    int dist[MAXN], parent[MAXN];
    clock_t start = clock();

    for (int i = 0; i < n; i++) {
        dist[i] = INF;
        parent[i] = -1;
    }
    dist[source] = 0;

    for (int iter = 0; iter < n - 1; iter++) {
        for (int u = 0; u < n; u++) {
            for (int v = 0; v < n; v++) {
                if (weight[u][v] != INF && dist[u] != INF &&
                    dist[u] + weight[u][v] < dist[v]) {
                    dist[v] = dist[u] + weight[u][v];
                    parent[v] = u;
                }
            }
        }
    }

    clock_t end = clock();
    double timeMs = ((double)(end - start)) * 1000.0 / CLOCKS_PER_SEC;

    int path[MAXN], pathLen = 0;
    if (dist[destination] < INF) {
        int cur = destination;
        while (cur != -1) {
            path[pathLen++] = cur;
            cur = parent[cur];
        }
        for (int i = 0; i < pathLen / 2; i++) {
            int temp = path[i];
            path[i] = path[pathLen - 1 - i];
            path[pathLen - 1 - i] = temp;
        }
    } else {
        path[0] = source;
        pathLen = 1;
    }

    printResult("Bellman-Ford", path, pathLen, dist[destination], timeMs);
}






/* ---------------- Floyd-Warshall Algorithm ----------------*/

void floydWarshall()
{
    int dist[MAXN][MAXN], next[MAXN][MAXN];
    clock_t start = clock();

    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            dist[i][j] = weight[i][j];
            next[i][j] = (i != j && weight[i][j] != INF) ? j : -1;
        }
    }

    for (int k = 0; k < n; k++) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (dist[i][k] != INF && dist[k][j] != INF &&
                    dist[i][k] + dist[k][j] < dist[i][j]) {
                    dist[i][j] = dist[i][k] + dist[k][j];
                    next[i][j] = next[i][k];
                }
            }
        }
    }

    clock_t end = clock();
    double timeMs = ((double)(end - start)) * 1000.0 / CLOCKS_PER_SEC;

    int path[MAXN], pathLen = 0;
    if (dist[source][destination] < INF) {
        int cur = source;
        path[pathLen++] = cur;
        while (cur != destination) {
            cur = next[cur][destination];
            path[pathLen++] = cur;
        }
    } else {
        path[0] = source;
        pathLen = 1;
    }

    printResult("Floyd-Warshall", path, pathLen, dist[source][destination], timeMs);
}

int main(int argc, char *argv[])
{
    if (argc < 3) {
        printf("Usage: %s <inputFile> <outputFile>\n", argv[0]);
        return 1;
    }

    readInput(argv[1]);

    outFile = fopen(argv[2], "w");
    if (outFile == NULL) {
        printf("Error: cannot open output file %s\n", argv[2]);
        return 1;
    }

    buildWeightMatrix(criteria);

    findAndPrintAllRoutes();

    if (algoChoice == 1) {
        dijkstra();
    } else if (algoChoice == 2) {
        bellmanFord();
    } else if (algoChoice == 3) {
        floydWarshall();
    } else if (algoChoice == 4) {
        dijkstra();
        bellmanFord();
        floydWarshall();
    }

    fclose(outFile);
    return 0;
}


